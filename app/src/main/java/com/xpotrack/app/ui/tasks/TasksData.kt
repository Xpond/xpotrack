package com.xpotrack.app.ui.tasks

import com.xpotrack.app.data.model.ReminderLevel
import com.xpotrack.app.data.model.Task
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// UI model for the tasks screens. VM maps domain Task → TaskRow.

data class TaskRow(
    val id: Long,
    val time: String,           // "HH:mm"
    val label: String,
    val level: ReminderLevel,
    val durationMin: Int,
    val done: Boolean = false,
    val dateEpochDay: Long = 0L,
)

fun Task.toRow(): TaskRow = TaskRow(
    id = id,
    time = time,
    label = title,
    level = level,
    durationMin = durationMin,
    done = isDone,
    dateEpochDay = dateEpochDay,
)

fun parseHHmm(s: String): Pair<Int, Int> {
    val (h, m) = s.split(":").map { it.toInt() }
    return h to m
}

fun formatTime12(time: String): String {
    val (h24, m) = parseHHmm(time)
    val h12 = ((h24 + 11) % 12) + 1
    return "%d:%02d %s".format(h12, m, if (h24 >= 12) "PM" else "AM")
}

// Human label for a repeat rule. Weekly resolves day-of-week from the task's
// own date so the row reads "Every Sunday" rather than a static word.
fun repeatLabel(rule: String, epochDay: Long = 0L): String = when (rule) {
    "daily" -> "Every day"
    "weekly" -> {
        val day = if (epochDay > 0L)
            LocalDate.ofEpochDay(epochDay).dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        else "week"
        "Every $day"
    }
    "weekdays" -> "Weekdays"
    else -> "Never"
}
