package com.xpotrack.app.data.model

// Pure domain task — no UI, no Room. Used by the alarm scheduler / receivers
// (milestone 8b) which can't depend on ui/ types. Repository emits these;
// UI maps to TaskRow at the screen edge.
data class Task(
    val id: Long = 0,
    val title: String,
    val time: String,           // "HH:mm" — display + scheduling source
    val level: ReminderLevel,
    val durationMin: Int,
    val notes: String = "",
    val category: String = "General",
    val isDone: Boolean = false,
    val reminderAt: Long = 0L,  // absolute epoch ms; 0 = unscheduled
    val createdAt: Long,
    val updatedAt: Long,
)
