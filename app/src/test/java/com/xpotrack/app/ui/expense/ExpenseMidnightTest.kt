package com.xpotrack.app.ui.expense

import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpenseMidnightTest {
    @Test fun ordinaryNoonWaitsTwelveHours() {
        assertEquals(
            12 * 60 * 60 * 1000L,
            millisUntilNextDay(ZonedDateTime.parse("2026-10-30T12:00:00+05:30[Asia/Kolkata]")),
        )
    }

    @Test fun springDayWaitsTwentyThreeHours() {
        assertEquals(
            23 * 60 * 60 * 1000L,
            millisUntilNextDay(ZonedDateTime.parse("2026-03-08T00:00:00-05:00[America/New_York]")),
        )
    }

    @Test fun autumnDayWaitsTwentyFiveHours() {
        assertEquals(
            25 * 60 * 60 * 1000L,
            millisUntilNextDay(ZonedDateTime.parse("2026-11-01T00:00:00-04:00[America/New_York]")),
        )
    }

    @Test fun justBeforeMidnightStillSuspends() {
        assertEquals(
            1L,
            millisUntilNextDay(ZonedDateTime.parse("2026-10-30T23:59:59.999999+05:30[Asia/Kolkata]")),
        )
    }
}
