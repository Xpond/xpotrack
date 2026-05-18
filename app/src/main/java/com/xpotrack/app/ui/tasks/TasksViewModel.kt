package com.xpotrack.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xpotrack.app.data.repo.TasksRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class TasksViewModel(private val repo: TasksRepository) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now(ZoneId.systemDefault()).toEpochDay())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    // All tasks across all dates — surfaces both the timeline (filtered by
    // selectedDate downstream) and the FAB tap that opens a new task on the
    // selected day.
    private val allRows: StateFlow<List<TaskRow>> = repo.observeAll()
        .map { list -> list.map { it.toRow() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tasks: StateFlow<List<TaskRow>> = combine(allRows, _selectedDate) { rows, date ->
        rows.filter { it.dateEpochDay == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Every day that has at least one task — feeds dots on the day chips and
    // the month picker so future-dated work is visible at a glance.
    val datesWithTasks: StateFlow<Set<Long>> = allRows
        .map { rows -> rows.asSequence().map { it.dateEpochDay }.filter { it > 0L }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun setSelectedDate(epochDay: Long) { _selectedDate.value = epochDay }

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    class Factory(private val repo: TasksRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TasksViewModel(repo) as T
    }
}
