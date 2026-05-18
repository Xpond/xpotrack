package com.xpotrack.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val bodyMarkdown: String,
    // categoryId is null for vault rows (they stay on the synthetic "Vault" label,
    // outside the category table). All non-locked rows must have a non-null id.
    val categoryId: Long? = null,
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
    val isDone: Boolean = false,
    val reminderAt: Long = 0L,  // absolute epoch ms; 0 = unscheduled (set in 8b)
    // Local-zone day number (LocalDate.toEpochDay). Pairs with `time` to form
    // an absolute moment. Migration v8→v9 backfills existing rows to today.
    val dateEpochDay: Long = 0L,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String,       // "#5EEAD4" etc.
    val sortOrder: Int,
)

@Entity(tableName = "meta")
data class MetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Entity(tableName = "quick_notes")
data class QuickNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val createdAt: Long,
    val expiresAt: Long,
)
