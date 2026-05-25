package com.xpotrack.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.xpotrack.app.data.model.Category
import com.xpotrack.app.data.repo.CategoryRepository
import com.xpotrack.app.data.repo.NotesRepository
import com.xpotrack.app.data.repo.QuickNotesRepository
import com.xpotrack.app.data.repo.QuickNotesRepository.Companion.LIFETIME_MS
import com.xpotrack.app.ui.quick.QuickRow
import com.xpotrack.app.ui.quick.remainingLabel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class NotesViewModel(
    private val repo: NotesRepository,
    categories: CategoryRepository,
    private val quick: QuickNotesRepository,
) : ViewModel() {

    private val tick = MutableStateFlow(System.currentTimeMillis())

    val categories: StateFlow<List<Category>> = categories.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val counts: StateFlow<NotesRepository.Counts> = repo.observeCounts()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            NotesRepository.Counts(0, 0, emptyMap()),
        )

    // null = all, 0L = uncategorized, >0 = that category id. Mirrors the UI's
    // filterId; we hold it here so a filter change re-sources the Pager via
    // flatMapLatest instead of filtering in memory.
    private val _filterId = MutableStateFlow<Long?>(null)
    val filterId: StateFlow<Long?> = _filterId.asStateFlow()
    fun setFilter(id: Long?) { _filterId.value = id }

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    fun setQuery(q: String) { _query.value = q }

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: Flow<PagingData<FeedItem.Note>> =
        combine(_filterId, _query) { f, q -> (f ?: -1L) to q }
            .flatMapLatest { (catFilter, q) ->
                repo.pagedNotes(catFilter, q.trim())
                    .map { data -> data.map { FeedItem.Note(it, sortKey = it.updatedAt) } }
            }
            .cachedIn(viewModelScope)

    // Quick notes still ride a normal Flow — small set, TTL-bounded, rendered
    // as a fixed block at the top of the LazyColumn above the paged notes.
    val quicks: StateFlow<List<FeedItem.Quick>> =
        combine(quick.observe(), tick) { quicks, now ->
            quicks.map { e ->
                FeedItem.Quick(
                    row = QuickRow(
                        id = e.id,
                        text = e.text,
                        leftLabel = remainingLabel((e.expiresAt - now).coerceAtLeast(0)),
                        pct = (((e.expiresAt - now).toDouble() / LIFETIME_MS) * 100)
                            .toInt().coerceIn(0, 100),
                        expiring = (e.expiresAt - now) < LIFETIME_MS * 15 / 100,
                    ),
                    sortKey = e.createdAt,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { quick.sweepExpired() }
        viewModelScope.launch {
            while (true) { delay(30_000); tick.value = System.currentTimeMillis() }
        }
    }

    fun delete(id: Int) { viewModelScope.launch { repo.delete(id) } }
    fun deleteQuick(id: Long) { viewModelScope.launch { quick.delete(id) } }
    fun keepQuick(id: Long) { viewModelScope.launch { quick.keep(id) } }

    class Factory(
        private val repo: NotesRepository,
        private val categories: CategoryRepository,
        private val quick: QuickNotesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NotesViewModel(repo, categories, quick) as T
    }
}

sealed class FeedItem {
    abstract val sortKey: Long
    data class Note(val row: NoteRow, override val sortKey: Long) : FeedItem()
    data class Quick(val row: QuickRow, override val sortKey: Long) : FeedItem()
}
