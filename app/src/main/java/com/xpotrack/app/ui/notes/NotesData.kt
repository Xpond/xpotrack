package com.xpotrack.app.ui.notes

// UI models for the notes screens. Repositories map their entities into these.
// Pure data classes — no Compose / Android imports — so the same shape is reused
// across UI, repository, and (eventually) preview / test code.

data class NoteRow(
    val id: Int,
    val title: String,
    val preview: String,
    val categoryId: Long,        // 0 = uncategorized
    val categoryName: String,    // resolved at repo edge; falls back to "Uncategorized"
    val when_: String,
)
