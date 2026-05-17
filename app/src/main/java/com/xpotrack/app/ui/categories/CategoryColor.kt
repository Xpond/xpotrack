package com.xpotrack.app.ui.categories

import androidx.compose.ui.graphics.Color
import com.xpotrack.app.ui.theme.XpTokens

// Parse "#RRGGBB" → Compose Color. Falls back to Ink3 on bad input so a broken
// row stays visible instead of crashing the whole screen.
fun parseHexColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: IllegalArgumentException) {
    XpTokens.Ink3
}
