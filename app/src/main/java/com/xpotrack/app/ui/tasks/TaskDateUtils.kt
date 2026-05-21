package com.xpotrack.app.ui.tasks

import com.xpotrack.app.data.model.ReminderLevel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private inline fun relativeOr(
    epochDay: Long,
    today: String,
    tomorrow: String,
    yesterday: String,
    fallback: (LocalDate) -> String,
): String {
    val now = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
    return when (epochDay - now) {
        0L -> today
        1L -> tomorrow
        -1L -> yesterday
        else -> fallback(LocalDate.ofEpochDay(epochDay))
    }
}

private val MONTH_DAY = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
private val EEE_MONTH_DAY = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

fun relativeDay(epochDay: Long): String =
    relativeOr(epochDay, "today", "tomorrow", "yesterday") { it.format(MONTH_DAY) }

fun dayLabel(epochDay: Long): String {
    if (epochDay <= 0L) return "Today"
    return relativeOr(epochDay, "Today", "Tomorrow", "Yesterday") { it.format(EEE_MONTH_DAY) }
}

fun dayOfWeekTitle(epochDay: Long): String =
    relativeOr(epochDay, "Today", "Tomorrow", "Yesterday") {
        it.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    }

fun levelLabel(level: ReminderLevel): String = when (level) {
    ReminderLevel.Silent -> "Silent"
    ReminderLevel.Notify -> "Notify"
    ReminderLevel.Alarm  -> "Alarm"
}

fun reminderSummary(level: ReminderLevel): String = when (level) {
    ReminderLevel.Silent -> "Silent"
    ReminderLevel.Notify -> "Notify on time"
    ReminderLevel.Alarm  -> "Alarm — ring for 60s"
}
