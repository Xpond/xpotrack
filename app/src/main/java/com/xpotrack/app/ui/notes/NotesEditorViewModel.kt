package com.xpotrack.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xpotrack.app.data.repo.CategoryRepository
import com.xpotrack.app.data.repo.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Edits a single note. Loads existing by id (0 = new). Save is suspending —
// callers (back button / system back) await it before navigating, so writes
// always commit before the list recomposes. No autosave-on-dispose; that races
// the ViewModel's own teardown on back navigation.
class NotesEditorViewModel(
    private val repo: NotesRepository,
    private val categories: CategoryRepository,
    private val noteId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (noteId > 0) {
                repo.getById(noteId)?.let { existing ->
                    _state.value = EditorState(
                        id = existing.id,
                        title = existing.title,
                        body = existing.preview,
                        categoryId = existing.categoryId,
                        categoryName = existing.categoryName,
                        loaded = true,
                        previewMode = true,
                    )
                } ?: run { _state.value = _state.value.copy(loaded = true) }
            } else {
                _state.value = EditorState(loaded = true, categoryId = 0L, categoryName = "Uncategorized")
            }
        }
    }

    fun onTitleChange(s: String) { _state.value = _state.value.copy(title = s) }
    fun onBodyChange(s: String)  { _state.value = _state.value.copy(body = s) }
    fun setPreview(on: Boolean)  { _state.value = _state.value.copy(previewMode = on) }

    // Toggle the checkbox on the Nth task-list line in body (0-indexed across
    // lines matching `- [ ] ` / `- [x] `). Called from preview when user taps.
    fun toggleTask(taskIndex: Int) {
        val lines = _state.value.body.split('\n').toMutableList()
        var seen = 0
        for (i in lines.indices) {
            val ln = lines[i]
            val unchecked = ln.startsWith("- [ ] ")
            val checked = ln.startsWith("- [x] ")
            if (!unchecked && !checked) continue
            if (seen == taskIndex) {
                lines[i] = if (unchecked) "- [x] " + ln.removePrefix("- [ ] ")
                           else "- [ ] " + ln.removePrefix("- [x] ")
                _state.value = _state.value.copy(body = lines.joinToString("\n"))
                return
            }
            seen++
        }
    }

    fun setCategory(id: Long) {
        viewModelScope.launch {
            if (id <= 0L) {
                _state.value = _state.value.copy(categoryId = 0L, categoryName = "Uncategorized")
                return@launch
            }
            val name = categories.getById(id)?.name ?: return@launch
            _state.value = _state.value.copy(categoryId = id, categoryName = name)
        }
    }

    suspend fun save() {
        val s = _state.value
        if (!s.loaded) return
        val hasContent = s.title.isNotBlank() || s.body.isNotBlank()
        if (s.id == 0 && !hasContent) return // empty new note: discard
        if (s.id > 0 && !hasContent) { repo.delete(s.id); return }
        repo.upsert(
            NoteRow(
                id = s.id,
                title = s.title.ifBlank { "Untitled" },
                preview = s.body,
                categoryId = s.categoryId,
                categoryName = s.categoryName,  // ignored on write — repo reads from FK
                when_ = "",
            )
        )
    }

    class Factory(
        private val repo: NotesRepository,
        private val categories: CategoryRepository,
        private val noteId: Int,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NotesEditorViewModel(repo, categories, noteId) as T
    }
}

data class EditorState(
    val id: Int = 0,
    val title: String = "",
    val body: String = "",
    val categoryId: Long = 0L,
    val categoryName: String = "Uncategorized",
    val loaded: Boolean = false,
    val previewMode: Boolean = false,
)
