package com.xpotrack.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
    private val noteId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    init {
        if (noteId > 0) {
            viewModelScope.launch {
                repo.getById(noteId)?.let { existing ->
                    _state.value = EditorState(
                        id = existing.id,
                        title = existing.title,
                        body = existing.preview,
                        category = existing.category,
                        isPinned = existing.isPinned,
                        loaded = true,
                    )
                } ?: run { _state.value = _state.value.copy(loaded = true) }
            }
        } else {
            _state.value = EditorState(loaded = true)
        }
    }

    fun onTitleChange(s: String) { _state.value = _state.value.copy(title = s) }
    fun onBodyChange(s: String)  { _state.value = _state.value.copy(body = s) }
    fun setPreview(on: Boolean)  { _state.value = _state.value.copy(previewMode = on) }

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
                category = s.category,
                when_ = "",
                words = 0,
                isPinned = s.isPinned,
            )
        )
    }

    class Factory(private val repo: NotesRepository, private val noteId: Int) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NotesEditorViewModel(repo, noteId) as T
    }
}

data class EditorState(
    val id: Int = 0,
    val title: String = "",
    val body: String = "",
    val category: String = "Inbox",
    val isPinned: Boolean = false,
    val loaded: Boolean = false,
    val previewMode: Boolean = false,
)
