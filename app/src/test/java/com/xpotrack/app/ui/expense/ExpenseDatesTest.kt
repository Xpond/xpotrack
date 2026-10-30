package com.xpotrack.app.ui.expense

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpenseDatesTest {
    private fun day(date: String) = LocalDate.parse(date).toEpochDay()

    @Test fun staleRowsAreHiddenImmediatelyWhenMonthChanges() {
        val oldRows = setOf(day("2026-10-29"), day("2026-10-30"))
        val selected = day("2026-09-01")
        assertEquals(
            listOf(selected),
            expenseDates(oldRows, selected, day("2026-10-30"), YearMonth.of(2026, 9)),
        )
    }

    @Test fun emptyMonthDoesNotShowStaleRowsOrToday() {
        assertEquals(
            emptyList<Long>(),
            expenseDates(setOf(day("2026-10-30")), null, day("2026-10-30"), YearMonth.of(2026, 9)),
        )
    }

    @Test fun datesAreUniqueAndNewestFirst() {
        val today = day("2026-10-30")
        val yesterday = day("2026-10-29")
        assertEquals(
            listOf(today, yesterday),
            expenseDates(setOf(yesterday, today), today, today, YearMonth.of(2026, 10)),
        )
    }
}
