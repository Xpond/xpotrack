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
    // Vault: when isLocked, bodyMarkdown is "" and ciphertext lives here.
    // Layout per VaultCrypto.encryptNote: [salt(16) | iv(12) | ct+tag].
    val encryptedBlob: ByteArray? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val time: String,           // "HH:mm" — local clock for display
    val level: String,          // ReminderLevel enum name
    val durationMin: Int,
    val notes: String = "",
    val category: String = "General",
    val isDone: Boolean = false,
    val reminderAt: Long = 0L,  // absolute epoch ms; 0 = unscheduled (set in 8b)
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "meta")
data class MetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)
