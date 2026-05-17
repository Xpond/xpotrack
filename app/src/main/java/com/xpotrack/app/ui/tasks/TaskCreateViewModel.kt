package com.xpotrack.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xpotrack.app.data.model.ReminderLevel
import com.xpotrack.app.data.model.Task
import com.xpotrack.app.data.repo.TasksRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TaskEditState(
    val id: Long = 0L,
    val title: String = "",
    val hour: Int = 16,         // 0..23
    val minute: Int = 0,        // 0..59
    val level: ReminderLevel = ReminderLevel.Alarm,
    val durationMin: Int = 30,
    val notes: String = "",
    val category: String = "General",
    val isDone: Boolean = false,
) {
    val timeHHmm: String get() = "%02d:%02d".format(hour, minute)
    val isNew: Boolean get() = id == 0L
}

class TaskCreateViewModel(
    private val repo: TasksRepository,
    private val id: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(TaskEditState())
    val state: StateFlow<TaskEditState> = _state.asStateFlow()

    init {
        if (id != 0L) viewModelScope.launch {
            repo.getById(id)?.let { _state.value = it.toEditState() }
        }
    }

    fun setTitle(s: String) { _state.value = _state.value.copy(title = s) }
    fun setHour(h: Int) { _state.value = _state.value.copy(hour = h.coerceIn(0, 23)) }
    fun setMinute(m: Int) { _state.value = _state.value.copy(minute = m.coerceIn(0, 59)) }
    fun setLevel(l: ReminderLevel) { _state.value = _state.value.copy(level = l) }
    fun setNotes(s: String) { _state.value = _state.value.copy(notes = s) }

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
                category = s.category,
                isDone = s.isDone,
                createdAt = 0L,         // repo preserves existing / sets now
                updatedAt = 0L,
            )
        )
    }

    class Factory(
        private val repo: TasksRepository,
        private val id: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TaskCreateViewModel(repo, id) as T
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
        category = category,
        isDone = isDone,
    )
}
