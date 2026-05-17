package com.xpotrack.app.ui.tasks

import com.xpotrack.app.data.model.ReminderLevel
import com.xpotrack.app.data.model.Task

// UI model for the tasks screens. VM maps domain Task → TaskRow.

data class TaskRow(
    val id: Long,
    val time: String,           // "HH:mm"
    val label: String,
    val level: ReminderLevel,
    val durationMin: Int,
    val done: Boolean = false,
)

fun Task.toRow(): TaskRow = TaskRow(
    id = id,
    time = time,
    label = title,
    level = level,
    durationMin = durationMin,
    done = isDone,
)

fun parseHHmm(s: String): Pair<Int, Int> {
    val (h, m) = s.split(":").map { it.toInt() }
    return h to m
}
