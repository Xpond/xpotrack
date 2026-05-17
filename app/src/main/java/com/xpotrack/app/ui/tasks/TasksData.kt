package com.xpotrack.app.ui.tasks

import com.xpotrack.app.data.model.ReminderLevel

// UI model for the tasks screens. Repository maps TaskEntity → TaskRow.

data class TaskRow(
    val id: Int,
    val time: String,           // "HH:mm"
    val label: String,
    val level: ReminderLevel,
    val durationMin: Int,
    val done: Boolean = false,
)

const val TimelineStartHour = 6
const val TimelineEndHour = 22
const val HourHeightDp = 56
val MinHeightPx: Float get() = HourHeightDp / 60f

fun parseHHmm(s: String): Pair<Int, Int> {
    val (h, m) = s.split(":").map { it.toInt() }
    return h to m
}

fun timeToOffsetDp(time: String): Float {
    val (h, m) = parseHHmm(time)
    return (h - TimelineStartHour) * HourHeightDp + m * MinHeightPx
}
