package com.xpotrack.app.ui.expense

import com.xpotrack.app.data.db.ExpenseDao
import com.xpotrack.app.data.db.ExpenseDayEntity
import com.xpotrack.app.data.repo.ExpenseRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModelTest {
    private lateinit var vm: ExpenseViewModel
    private fun day(date: String) = LocalDate.parse(date).toEpochDay()

    @Before fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        vm = ExpenseViewModel(ExpenseRepository(EmptyExpenseDao()))
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test fun returningOnANewDayOpensToday() {
        val tomorrow = vm.today.value + 1
        vm.onEnter(tomorrow)
        assertEquals(tomorrow, vm.today.value)
        assertEquals(tomorrow, vm.openDay.value)
    }

    @Test fun returningAcrossNewYearChangesMonth() {
        vm.onEnter(day("2026-12-31"))
        vm.onEnter(day("2027-01-01"))
        assertEquals(YearMonth.of(2027, 1), vm.month.value)
        assertEquals(day("2027-01-01"), vm.openDay.value)
    }

    @Test fun midnightRefreshPreservesDraftUntilNextEntry() {
        val original = vm.today.value
        vm.refreshToday(original + 1)
        assertEquals(original + 1, vm.today.value)
        assertEquals(original, vm.openDay.value)
        vm.onEnter(original + 1)
        assertEquals(original + 1, vm.openDay.value)
    }

    @Test fun browsingHistoryIsPreservedAfterMidnight() {
        val tomorrow = vm.today.value + 1
        val historical = day("2025-09-12")
        vm.pick(historical)
        vm.onEnter(tomorrow)
        assertEquals(tomorrow, vm.today.value)
        assertEquals(historical, vm.openDay.value)
        assertEquals(YearMonth.of(2025, 9), vm.month.value)
    }

    @Test fun collapsedCurrentMonthFollowsTodayOnReturn() {
        val original = vm.today.value
        vm.toggle(original)
        vm.onEnter(original + 1)
        assertEquals(original + 1, vm.openDay.value)
    }

    @Test fun returningOnSameDayKeepsCollapsedState() {
        val original = vm.today.value
        vm.toggle(original)
        vm.onEnter(original)
        assertEquals(null, vm.openDay.value)
    }
}

private class EmptyExpenseDao : ExpenseDao {
    override fun observeRange(from: Long, to: Long) = flowOf(emptyList<ExpenseDayEntity>())
    override fun observeDates() = flowOf(emptyList<Long>())
    override suspend fun getText(day: Long): String? = null
    override suspend fun upsert(row: ExpenseDayEntity) = error("Not used in date-navigation tests")
    override suspend fun delete(day: Long) = error("Not used in date-navigation tests")
}
