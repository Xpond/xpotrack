package com.xpotrack.app.ui.categories

import androidx.compose.ui.graphics.Color
import com.xpotrack.app.ui.theme.XpTokens
import java.util.concurrent.ConcurrentHashMap

// Parse "#RRGGBB" → Compose Color. Falls back to Ink3 on bad input so a broken
// row stays visible instead of crashing the whole screen.
//
// Called per visible row per recompose on hot paths (NotesChronoView, filter
// bar). Only the success path is cached — ~10 distinct category colors exist
// app-wide, so reparsing the same string is pure waste. The Ink3 fallback
// stays uncached so it remains reactive to palette swaps (Ink3 is mutableState).
private val cache = ConcurrentHashMap<String, Color>()

fun parseHexColor(hex: String): Color = cache[hex] ?: try {
    Color(android.graphics.Color.parseColor(hex)).also { cache[hex] = it }
} catch (_: IllegalArgumentException) {
    XpTokens.Ink3
}
