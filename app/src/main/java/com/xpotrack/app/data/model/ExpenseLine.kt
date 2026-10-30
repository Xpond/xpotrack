package com.xpotrack.app.data.model

// One expense per line with the amount at the end: "Lunch 260", "Groceries
// 1,240.50", "Chai ₹60", "Lunch Rs.260", "Bus 20 rupees". The number must stand
// alone (start of line, or after a space, ₹ or a standalone rs/inr), so
// "Covid19" and "Covers260" aren't amounts. Lines without a trailing amount are
// plain notes and count as zero.
private val AMOUNT = Regex(
    """(?:(?<!\S)(?:rs\.?|inr)\s*|(?<![^\s₹]))(\d[\d,]*(?:\.\d{1,2})?)\s*(?:₹|rs\.?|inr|rupees?|/-)?\s*$""",
    RegexOption.IGNORE_CASE,
)

data class ExpenseLine(val label: String, val paise: Long, val amountRange: IntRange)

fun parseExpenseLine(line: String): ExpenseLine? {
    val match = AMOUNT.find(line) ?: return null
    val g = match.groups[1]!!
    val paise = runCatching {
        g.value.replace(",", "").toBigDecimal().movePointRight(2).longValueExact()
    }.getOrNull() ?: return null
    return ExpenseLine(line.substring(0, match.range.first).trim().trimEnd('₹').trim(), paise, g.range)
}

fun expenseTotalPaise(text: String): Long =
    text.split('\n').sumOf { parseExpenseLine(it)?.paise ?: 0L }
