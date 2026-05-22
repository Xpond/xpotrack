package com.xpotrack.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xpotrack.app.data.repo.CategoryRepository
import com.xpotrack.app.data.repo.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Edits a single note. Loads existing by id (0 = new). Save is suspending —
// callers (back button / system back) await it before navigating, so writes
// always commit before the list recomposes. No autosave-on-dispose; that races
// the ViewModel's own teardown on back navigation.
class NotesEditorViewModel(
    private val repo: NotesRepository,
    private val categories: CategoryRepository,
    private val noteId: Int,
    private val initialCategoryId: Long = 0L,
) : ViewModel() {

    // Internal mutable state holds only what the user types/picks. The category
    // label + color are resolved live from the categories flow below, so a
    // delete/rename/recolor of the selected category reflects in the editor
    // without any manual sync. save() also re-resolves so a dangling categoryId
    // (selected category deleted while editor was open) never gets persisted.
    private val _state = MutableStateFlow(EditorState())

    val state: StateFlow<EditorState> = combine(_state, categories.observeAll()) { s, cats ->
        if (s.categoryId <= 0L) {
            s.copy(categoryName = "Uncategorized", categoryColorHex = null)
        } else {
            val c = cats.firstOrNull { it.id == s.categoryId }
            if (c == null) s.copy(categoryId = 0L, categoryName = "Uncategorized", categoryColorHex = null)
            else s.copy(categoryName = c.name, categoryColorHex = c.colorHex)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditorState())

    // Snapshot of the loaded note. save() compares against this so opening a
    // note and backing out without edits doesn't bump updatedAt.
    private var pristine: Triple<String, String, Long>? = null

    init {
        viewModelScope.launch {
            if (noteId > 0) {
                repo.getById(noteId)?.let { existing ->
                    _state.value = EditorState(
                        id = existing.id,
                        title = existing.title,
                        body = existing.preview,
                        categoryId = existing.categoryId,
                        loaded = true,
                        previewMode = true,
                    )
                    pristine = Triple(existing.title, existing.preview, existing.categoryId)
                } ?: run { _state.value = _state.value.copy(loaded = true) }
            } else {
                _state.value = EditorState(loaded = true, categoryId = initialCategoryId)
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

    fun setCategory(id: Long) { _state.value = _state.value.copy(categoryId = id) }

    suspend fun save() {
        val s = _state.value
        if (!s.loaded) return
        val hasContent = s.title.isNotBlank() || s.body.isNotBlank()
        if (s.id == 0 && !hasContent) return // empty new note: discard
        if (s.id > 0 && !hasContent) { repo.delete(s.id); return }
        // Re-resolve the category at save time so a deleted one falls back to
        // Uncategorized rather than persisting a dangling id.
        val live = if (s.categoryId <= 0L) null else categories.getById(s.categoryId)
        val finalId = live?.id ?: 0L
        pristine?.let { (t, b, c) ->
            if (t == s.title && b == s.body && c == finalId) return
        }
        repo.upsert(
            NoteRow(
                id = s.id,
                title = s.title.ifBlank { "Untitled" },
                preview = s.body,
                categoryId = finalId,
                categoryName = live?.name ?: "Uncategorized",
                categoryColorHex = null,
                when_ = "",
                updatedAt = 0L,
            )
        )
    }

    class Factory(
        private val repo: NotesRepository,
        private val categories: CategoryRepository,
        private val noteId: Int,
        private val initialCategoryId: Long = 0L,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NotesEditorViewModel(repo, categories, noteId, initialCategoryId) as T
    }
}

data class EditorState(
    val id: Int = 0,
    val title: String = "",
    val body: String = "",
    val categoryId: Long = 0L,
    val categoryName: String = "Uncategorized",
    val categoryColorHex: String? = null,
    val loaded: Boolean = false,
    val previewMode: Boolean = false,
)
