package com.xpotrack.app.data.model

// One expense per line with the amount at the end: "Lunch 260", "Groceries
// 1,240.50", "Chai ₹60", "Auto 120rs". The number must stand alone (start of
// line, or after a space or ₹), so "Covid19" isn't an amount. Lines without a
// trailing amount are plain notes and count as zero.
private val AMOUNT = Regex("""(?<![^\s₹])(\d[\d,]*(?:\.\d{1,2})?)\s*(?:rs\.?|/-)?\s*$""", RegexOption.IGNORE_CASE)

data class ExpenseLine(val label: String, val paise: Long, val amountRange: IntRange)

fun parseExpenseLine(line: String): ExpenseLine? {
    val g = AMOUNT.find(line)?.groups?.get(1) ?: return null
    val paise = runCatching {
        g.value.replace(",", "").toBigDecimal().movePointRight(2).longValueExact()
    }.getOrNull() ?: return null
    return ExpenseLine(line.substring(0, g.range.first).trim().trimEnd('₹').trim(), paise, g.range)
}

fun expenseTotalPaise(text: String): Long =
    text.split('\n').sumOf { parseExpenseLine(it)?.paise ?: 0L }
