package com.xpotrack.app.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xpotrack.app.data.model.Category
import com.xpotrack.app.data.repo.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryEdit(
    val id: Long = 0L,           // 0 = a new draft
    val name: String = "",
    val colorHex: String = "#67E8F9",
) {
    val isNew: Boolean get() = id == 0L
}

data class PendingDelete(val id: Long, val name: String, val noteCount: Int)

class CategoryManagerViewModel(
    private val repo: CategoryRepository,
) : ViewModel() {

    val categories: StateFlow<List<Category>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Active inline editor (rename or new). Null = no editor open.
    private val _edit = MutableStateFlow<CategoryEdit?>(null)
    val edit: StateFlow<CategoryEdit?> = _edit.asStateFlow()

    private val _pendingDelete = MutableStateFlow<PendingDelete?>(null)
    val pendingDelete: StateFlow<PendingDelete?> = _pendingDelete.asStateFlow()

    // Tracks the id of the category created in the current manager session, so
    // callers (e.g. the editor's category picker) can auto-select it on close.
    // Reset on session re-entry via `clearLastCreated()`.
    private val _lastCreated = MutableStateFlow(0L)
    val lastCreated: StateFlow<Long> = _lastCreated.asStateFlow()
    fun clearLastCreated() { _lastCreated.value = 0L }

    fun startCreate() { _edit.value = CategoryEdit() }
    fun startRename(c: Category) { _edit.value = CategoryEdit(c.id, c.name, c.colorHex) }
    fun cancelEdit() { _edit.value = null }

    fun editName(s: String) { _edit.value = _edit.value?.copy(name = s) }
    fun editColor(hex: String) { _edit.value = _edit.value?.copy(colorHex = hex) }

    fun commitEdit() {
        val e = _edit.value ?: return
        if (e.name.isBlank()) { _edit.value = null; return }
        viewModelScope.launch {
            if (e.isNew) {
                val newId = repo.add(e.name, e.colorHex)
                _lastCreated.value = newId
            } else {
                repo.rename(e.id, e.name)
                repo.recolor(e.id, e.colorHex)
            }
            _edit.value = null
        }
    }

    fun askDelete(c: Category) {
        viewModelScope.launch {
            val notes = repo.noteUsage(c.id)
            _pendingDelete.value = PendingDelete(c.id, c.name, notes)
        }
    }

    fun confirmDelete() {
        val p = _pendingDelete.value ?: return
        viewModelScope.launch {
            repo.deleteAndUncategorize(p.id)
            _pendingDelete.value = null
        }
    }

    fun cancelDelete() { _pendingDelete.value = null }

    class Factory(private val repo: CategoryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CategoryManagerViewModel(repo) as T
    }
}
