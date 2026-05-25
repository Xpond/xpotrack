package com.xpotrack.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.xpotrack.app.data.db.QuickNoteEntity
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class NotesViewModel(
    private val repo: NotesRepository,
    categories: CategoryRepository,
    private val quick: QuickNotesRepository,
) : ViewModel() {

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
    fun setFilter(id: Long?) {
        if (_filterId.value != id) _isStale.value = true
        _filterId.value = id
    }

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // True between a query/filter change and the UI confirming that the new
    // snapshot has loaded. Set by setQuery/setFilter; cleared by the UI via
    // markFresh() once LazyPagingItems' loadState has cycled. UI-driven clear
    // is needed because PagingData is emitted before SQLite has actually
    // produced rows — important at 1M+ notes where the unfiltered initial
    // load takes hundreds of ms.
    private val _isStale = MutableStateFlow(false)
    val isStale: StateFlow<Boolean> = _isStale.asStateFlow()
    fun markFresh() { _isStale.value = false }

    fun setQuery(q: String) {
        if (_query.value != q) _isStale.value = true
        _query.value = q
    }

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
    // The 30s ticker only runs while there's at least one quick note to count
    // down; flatMapLatest cancels it when the list goes empty and restarts it
    // when one appears.
    @OptIn(ExperimentalCoroutinesApi::class)
    val quicks: StateFlow<List<FeedItem.Quick>> =
        quick.observe()
            .flatMapLatest { qs ->
                if (qs.isEmpty()) flowOf(emptyList())
                else minuteTicks().map { now -> qs.map { it.toFeedItem(now) } }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun QuickNoteEntity.toFeedItem(now: Long) = FeedItem.Quick(
        row = QuickRow(
            id = id,
            text = text,
            leftLabel = remainingLabel((expiresAt - now).coerceAtLeast(0)),
            pct = (((expiresAt - now).toDouble() / LIFETIME_MS) * 100)
                .toInt().coerceIn(0, 100),
            expiring = (expiresAt - now) < LIFETIME_MS * 15 / 100,
        ),
        sortKey = createdAt,
    )

    private fun minuteTicks(): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000)
        }
    }

    init {
        viewModelScope.launch { quick.sweepExpired() }
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
