package com.xpotrack.app.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xpotrack.app.data.repo.ExpenseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class ExpenseViewModel(private val repo: ExpenseRepository) : ViewModel() {

    private val _month = MutableStateFlow(YearMonth.now(ZoneId.systemDefault()))
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    // The one day expanded for editing. Today on entry; null when collapsed.
    private val _openDay = MutableStateFlow<Long?>(LocalDate.now(ZoneId.systemDefault()).toEpochDay())
    val openDay: StateFlow<Long?> = _openDay.asStateFlow()

    // epochDay → text for every logged day in the shown month.
    @OptIn(ExperimentalCoroutinesApi::class)
    val days: StateFlow<Map<Long, String>> = _month
        .flatMapLatest { m -> repo.observeRange(m.atDay(1).toEpochDay(), m.atEndOfMonth().toEpochDay()) }
        .map { rows -> rows.associate { it.dateEpochDay to it.text } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // Dots on the month picker.
    val loggedDates: StateFlow<Set<Long>> = repo.observeDates()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // Every keystroke saves. Mutex is FIFO, so writes land in typing order and
    // a reload of a day waits for that day's pending writes.
    private val lock = Mutex()

    fun save(day: Long, text: String) {
        viewModelScope.launch { lock.withLock { repo.save(day, text) } }
    }

    suspend fun load(day: Long): String = lock.withLock { repo.get(day) }

    fun toggle(day: Long) {
        _openDay.value = if (_openDay.value == day) null else day
    }

    fun pick(day: Long) {
        _month.value = YearMonth.from(LocalDate.ofEpochDay(day))
        _openDay.value = day
    }

    class Factory(private val repo: ExpenseRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ExpenseViewModel(repo) as T
    }
}
