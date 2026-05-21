package com.xpotrack.app.data.repo

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val zone: ZoneId = ZoneId.systemDefault()
private val monthDay = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
private val weekday = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)

internal fun formatWhen(updatedAt: Long, today: LocalDate = LocalDate.now(zone)): String {
    val d = Instant.ofEpochMilli(updatedAt).atZone(zone).toLocalDate()
    return when (val days = ChronoUnit.DAYS.between(d, today).toInt()) {
        0 -> "Today"
        1 -> "Yesterday"
        in 2..6 -> d.format(weekday)
        else -> d.format(monthDay)
    }
}
