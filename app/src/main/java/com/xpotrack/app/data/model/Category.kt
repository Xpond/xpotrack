package com.xpotrack.app.data.model

// Pure domain category — no UI, no Room. UI screens, the picker, and the
// manager all consume this; repos map CategoryEntity → Category at the edge.
data class Category(
    val id: Long,
    val name: String,
    val colorHex: String,       // "#5EEAD4" etc.
    val sortOrder: Int,
)
