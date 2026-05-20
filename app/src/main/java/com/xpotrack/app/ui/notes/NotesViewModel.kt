package com.xpotrack.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xpotrack.app.data.model.Category
import com.xpotrack.app.data.repo.CategoryRepository
import com.xpotrack.app.data.repo.NotesRepository
import com.xpotrack.app.data.repo.QuickNotesRepository
import com.xpotrack.app.data.repo.QuickNotesRepository.Companion.LIFETIME_MS
import com.xpotrack.app.ui.quick.QuickRow
import com.xpotrack.app.ui.quick.remainingLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(
    private val repo: NotesRepository,
    categories: CategoryRepository,
    private val quick: QuickNotesRepository,
) : ViewModel() {

    // 30s tick keeps countdown chips fresh while the index is on screen.
    private val tick = MutableStateFlow(System.currentTimeMillis())

    val categories: StateFlow<List<Category>> = categories.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Two separate streams so the index can render notes the instant they're
    // ready instead of waiting on quick notes too. Combine would block first
    // emission until every source has emitted at least once. The screen
    // interleaves them by sortKey at render time.
    val notes: StateFlow<List<FeedItem.Note>> =
        repo.observeAll()
            .map { rows -> rows.map { FeedItem.Note(it, sortKey = it.updatedAt) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
        // Sweep on open + 30s tick. Mirrors what the old queue VM did, but on the
        // index — quick notes vanish in place once their row expires.
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
