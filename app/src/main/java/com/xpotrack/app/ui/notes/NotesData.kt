package com.xpotrack.app.ui.notes

// UI models for the notes screens. Repositories map their entities into these.
// Pure data classes — no Compose / Android imports — so the same shape is reused
// across UI, repository, and (eventually) preview / test code.

data class NoteRow(
    val id: Int,
    val title: String,
    val preview: String,
    val category: String,
    val when_: String,
    val words: Int,
    val recency: Int,
)

data class Category(val name: String, val isCustom: Boolean)

// Pinned still hardcoded by id; will be replaced by NoteEntity.isPinned in milestone 6.
val PinnedIds: Set<Int> = setOf(1, 8)

val Categories: List<Category> = listOf(
    Category("Personal", isCustom = false),
    Category("Work", isCustom = false),
    Category("Ideas", isCustom = false),
    Category("Inbox", isCustom = false),
    Category("Trip", isCustom = true),
    Category("Essay", isCustom = true),
    Category("Recipe", isCustom = true),
)
