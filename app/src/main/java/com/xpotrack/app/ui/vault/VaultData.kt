package com.xpotrack.app.ui.vault

// Plain data, no Compose imports — VaultRepository depends on these.

data class LockedNoteRow(
    val id: Long,
    val title: String,
    val category: String,
    val when_: String,
)

data class LockedNote(
    val id: Long,
    val title: String,
    val category: String,
    val body: String,
    val updatedAt: Long,
)
