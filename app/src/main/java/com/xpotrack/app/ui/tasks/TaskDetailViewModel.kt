package com.xpotrack.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xpotrack.app.data.model.Task
import com.xpotrack.app.data.repo.NotesRepository
import com.xpotrack.app.data.repo.TasksRepository
import com.xpotrack.app.ui.notes.NoteRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskDetailState(
    val loaded: Boolean = false,
    val task: Task? = null,
    val totalToday: Int = 0,
    val indexToday: Int = 0,    // 1-based "N of M"; 0 until resolved
)

class TaskDetailViewModel(
    private val repo: TasksRepository,
    notesRepo: NotesRepository,
    private val id: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(TaskDetailState())
    val state: StateFlow<TaskDetailState> = _state.asStateFlow()

    // Separate from task state so typing doesn't churn the whole detail screen.
    // Initialized once on load; saved on back navigation.
    private val _notesDraft = MutableStateFlow("")
    val notesDraft: StateFlow<String> = _notesDraft.asStateFlow()

    // Live note list — feeds the link picker.
    val allNotes: StateFlow<List<NoteRow>> = notesRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val wasFirstLoad = !_state.value.loaded
            val task = repo.getById(id)
            val tasks = repo.observeAll().first()
            // Counter is "N of M for this task's day" — filter to siblings on
            // the same date so opening a future-dated task doesn't read
            // "12 of 47 today".
            val sameDay = tasks.filter { it.dateEpochDay == task?.dateEpochDay }
            val sorted = sameDay.sortedBy { val (h, m) = parseHHmm(it.time); h * 60 + m }
            val idx = sorted.indexOfFirst { it.id == id }
            _state.value = TaskDetailState(
                loaded = true,
                task = task,
                totalToday = sorted.size,
                indexToday = if (idx >= 0) idx + 1 else 0,
            )
            // Only seed the draft on first load — refreshes after sheet edits
            // would otherwise clobber an in-progress notes edit.
            if (task != null && wasFirstLoad) _notesDraft.value = task.notes
        }
    }

    fun onNotesChange(s: String) { _notesDraft.value = s }

    suspend fun saveNotesIfDirty() {
        val task = _state.value.task ?: return
        if (task.isDone) return
        if (task.notes == _notesDraft.value) return
        repo.upsert(task.copy(notes = _notesDraft.value))
    }

    fun setLinkedNote(noteId: Long?) {
        val task = _state.value.task ?: return
        if (task.linkedNoteId == noteId) return
        viewModelScope.launch {
            // Persist current draft alongside the link change so we don't lose
            // an in-progress notes edit when the upsert below runs.
            val notes = if (!task.isDone) _notesDraft.value else task.notes
            repo.upsert(task.copy(linkedNoteId = noteId, notes = notes))
            refresh()
        }
    }

    suspend fun markDone() { repo.markDone(id) }
    suspend fun delete()   { repo.delete(id) }

    class Factory(
        private val repo: TasksRepository,
        private val notesRepo: NotesRepository,
        private val id: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TaskDetailViewModel(repo, notesRepo, id) as T
    }
}
