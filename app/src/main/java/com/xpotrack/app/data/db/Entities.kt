package com.xpotrack.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val bodyMarkdown: String,
    val category: String,
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val time: String,           // "HH:mm"
    val level: String,          // "SILENT" / "NOTIFY" / "ALARM"
    val durationMin: Int,
    val isDone: Boolean = false,
    val createdAt: Long,
)

@Entity(tableName = "meta")
data class MetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)
