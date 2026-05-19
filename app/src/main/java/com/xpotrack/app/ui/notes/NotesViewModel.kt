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

    // One stream, two sources, interleaved by timestamp. Quick notes carry
    // their full row shape so the index can render the same chip/keep UI the
    // old queue screen used.
    val feed: StateFlow<List<FeedItem>> =
        combine(repo.observeAll(), quick.observe(), tick) { notes, quicks, now ->
            val n = notes.map { FeedItem.Note(it, sortKey = it.updatedAt) }
            val q = quicks.map { e ->
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
            (n + q).sortedByDescending { it.sortKey }
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
