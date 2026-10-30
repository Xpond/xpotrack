package com.xpotrack.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseLineTest {
    private fun assertAmount(line: String, label: String, paise: Long) {
        val parsed = parseExpenseLine(line)
        assertEquals(line, label, parsed?.label)
        assertEquals(line, paise, parsed?.paise)
    }

    @Test fun plainAmounts() {
        assertAmount("Lunch 260", "Lunch", 26_000)
        assertAmount("Groceries 1,240.50", "Groceries", 124_050)
        assertAmount("Tip 12.5", "Tip", 1_250)
        assertAmount("260", "", 26_000)
    }

    @Test fun currencyBeforeAmount() {
        assertAmount("Chai ₹60", "Chai", 6_000)
        assertAmount("Chai ₹ 60", "Chai", 6_000)
        assertAmount("Chai₹60", "Chai", 6_000)
        assertAmount("Lunch Rs.260", "Lunch", 26_000)
        assertAmount("Lunch rs 260", "Lunch", 26_000)
        assertAmount("Lunch RS260", "Lunch", 26_000)
        assertAmount("Lunch INR 260", "Lunch", 26_000)
        assertAmount("Rs 60", "", 6_000)
    }

    @Test fun currencyAfterAmount() {
        assertAmount("Auto 120rs", "Auto", 12_000)
        assertAmount("Auto 120 Rs.", "Auto", 12_000)
        assertAmount("Auto 120/-", "Auto", 12_000)
        assertAmount("Chai 60₹", "Chai", 6_000)
        assertAmount("Chai 60 ₹", "Chai", 6_000)
        assertAmount("Bus 20 rupees", "Bus", 2_000)
        assertAmount("Bus 1 rupee", "Bus", 100)
        assertAmount("Bus 20 inr", "Bus", 2_000)
    }

    @Test fun linesWithoutAStandaloneAmountAreNotes() {
        listOf(
            "Paid by Ravi", "", "Covid19", "Covers260", "Refund -200",
            "Lunch 1.234", "Rs", "99999999999999999999",
        ).forEach { assertNull(it, parseExpenseLine(it)) }
    }

    @Test fun amountRangeCoversOnlyTheDigits() {
        listOf("Lunch Rs.1,260.50", "Lunch 1,260.50 rupees").forEach {
            assertEquals("1,260.50", it.substring(parseExpenseLine(it)!!.amountRange))
        }
    }

    @Test fun totalAddsAmountLinesAndIgnoresNotes() {
        assertEquals(38_050L, expenseTotalPaise("Lunch 260\nPaid by Ravi\n\nBus Rs.120.50"))
    }
}
