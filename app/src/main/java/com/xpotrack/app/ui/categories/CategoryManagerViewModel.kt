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

// Palette swatches mirrored from category-manager.jsx.
val CategoryPalette = listOf("#67E8F9", "#86EFAC", "#FCD34D", "#FCA17D", "#A78BFA", "#FCA5A5")

data class CategoryEdit(
    val id: Long = 0L,           // 0 = a new draft
    val name: String = "",
    val colorHex: String = CategoryPalette[0],
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

    fun startCreate() { _edit.value = CategoryEdit() }
    fun startRename(c: Category) { _edit.value = CategoryEdit(c.id, c.name, c.colorHex) }
    fun cancelEdit() { _edit.value = null }

    fun editName(s: String) { _edit.value = _edit.value?.copy(name = s) }
    fun editColor(hex: String) { _edit.value = _edit.value?.copy(colorHex = hex) }

    fun commitEdit() {
        val e = _edit.value ?: return
        if (e.name.isBlank()) { _edit.value = null; return }
        viewModelScope.launch {
            if (e.isNew) repo.add(e.name, e.colorHex)
            else {
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
