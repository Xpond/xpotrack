package com.xpotrack.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xpotrack.app.data.model.ReminderLevel
import com.xpotrack.app.data.model.Task
import com.xpotrack.app.data.repo.NotesRepository
import com.xpotrack.app.data.repo.TasksRepository
import com.xpotrack.app.ui.notes.NoteRow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class TaskEditState(
    val id: Long = 0L,
    val title: String = "",
    val hour: Int = 0,          // 0..23
    val minute: Int = 0,        // 0..59
    val level: ReminderLevel = ReminderLevel.Alarm,
    val durationMin: Int = 30,
    val notes: String = "",
    val isDone: Boolean = false,
    val dateEpochDay: Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay(),
    val repeat: String = "none",
    val linkedNoteId: Long? = null,
) {
    val timeHHmm: String get() = "%02d:%02d".format(hour, minute)
    val isNew: Boolean get() = id == 0L
}

class TaskCreateViewModel(
    private val repo: TasksRepository,
    private val notesRepo: NotesRepository,
    private val id: Long,
    initialDate: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(
        LocalTime.now().let { now ->
            TaskEditState(dateEpochDay = initialDate, hour = now.hour, minute = now.minute)
        }
    )
    val state: StateFlow<TaskEditState> = _state.asStateFlow()

    // Link-note picker. Bounded search via SQLite — see
    // TaskDetailViewModel for the same pattern + rationale.
    private val _pickerQuery = MutableStateFlow("")
    val pickerQuery: StateFlow<String> = _pickerQuery.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val pickerResults: StateFlow<List<NoteRow>> = _pickerQuery
        .debounce(150)
        .flatMapLatest { notesRepo.searchForPicker(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Title chip for the currently-linked note — resolved by id, refetched
    // when the linked id changes.
    private val _linkedNote = MutableStateFlow<NoteRow?>(null)
    val linkedNote: StateFlow<NoteRow?> = _linkedNote.asStateFlow()

    fun setPickerQuery(q: String) { _pickerQuery.value = q }

    init {
        if (id != 0L) viewModelScope.launch {
            repo.getById(id)?.let {
                _state.value = it.toEditState()
                _linkedNote.value = it.linkedNoteId?.let { lid -> notesRepo.getById(lid.toInt()) }
            }
        }
    }

    fun setTitle(s: String) { _state.value = _state.value.copy(title = s) }
    fun setHour(h: Int) { _state.value = _state.value.copy(hour = h.coerceIn(0, 23)) }
    fun setMinute(m: Int) { _state.value = _state.value.copy(minute = m.coerceIn(0, 59)) }
    fun setLevel(l: ReminderLevel) { _state.value = _state.value.copy(level = l) }
    fun setNotes(s: String) { _state.value = _state.value.copy(notes = s) }
    fun setDate(epochDay: Long) { _state.value = _state.value.copy(dateEpochDay = epochDay) }
    fun setRepeat(rule: String) { _state.value = _state.value.copy(repeat = rule) }
    fun setLinkedNote(noteId: Long?) {
        _state.value = _state.value.copy(linkedNoteId = noteId)
        viewModelScope.launch {
            _linkedNote.value = noteId?.let { notesRepo.getById(it.toInt()) }
        }
    }

    suspend fun save(): Long? {
        val s = _state.value
        if (s.title.isBlank()) return null
        return repo.upsert(
            Task(
                id = s.id,
                title = s.title.trim(),
                time = s.timeHHmm,
                level = s.level,
                durationMin = s.durationMin,
                notes = s.notes,
                isDone = s.isDone,
                dateEpochDay = s.dateEpochDay,
                repeat = s.repeat,
                linkedNoteId = s.linkedNoteId,
                createdAt = 0L,         // repo preserves existing / sets now
                updatedAt = 0L,
            )
        )
    }

    class Factory(
        private val repo: TasksRepository,
        private val notesRepo: NotesRepository,
        private val id: Long,
        private val initialDate: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TaskCreateViewModel(repo, notesRepo, id, initialDate) as T
    }
}

private fun Task.toEditState(): TaskEditState {
    val (h, m) = parseHHmm(time)
    return TaskEditState(
        id = id,
        title = title,
        hour = h,
        minute = m,
        level = level,
        durationMin = durationMin,
        notes = notes,
        isDone = isDone,
        dateEpochDay = dateEpochDay,
        repeat = repeat,
        linkedNoteId = linkedNoteId,
    )
}
