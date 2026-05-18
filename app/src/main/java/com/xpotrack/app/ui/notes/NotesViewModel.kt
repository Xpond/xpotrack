package com.xpotrack.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xpotrack.app.data.model.Category
import com.xpotrack.app.data.repo.CategoryRepository
import com.xpotrack.app.data.repo.NotesRepository
import com.xpotrack.app.data.repo.QuickNotesRepository
import com.xpotrack.app.ui.quick.remainingLabel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(
    private val repo: NotesRepository,
    categories: CategoryRepository,
    quick: QuickNotesRepository,
) : ViewModel() {

    val notes: StateFlow<List<NoteRow>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> = categories.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val quickSummary: StateFlow<QuickSummary> = quick.observe()
        .map { rows ->
            val now = System.currentTimeMillis()
            val oldestExpiresAt = rows.minByOrNull { it.expiresAt }?.expiresAt
            QuickSummary(
                count = rows.size,
                oldestLeft = oldestExpiresAt?.let { remainingLabel(it - now) },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuickSummary(0, null))

    fun delete(id: Int) {
        viewModelScope.launch { repo.delete(id) }
    }

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

data class QuickSummary(val count: Int, val oldestLeft: String?)
