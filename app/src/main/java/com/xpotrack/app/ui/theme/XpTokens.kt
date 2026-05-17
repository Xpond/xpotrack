package com.xpotrack.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Mirrors misc/mockups/screens/system.jsx — single source of truth for *colors and shapes*.
// Typography lives in XpTypography.kt (Material 3 type scale). Per-screen spacing is written
// inline at the call site, not centralized here, because the mockup uses variable padding
// values that resist a small set of named constants.
object XpTokens {
    // Surfaces
    val Bg = Color(0xFF06100F)
    val Surface1 = Color(0xFF0C1A19)
    val Surface2 = Color(0xFF122524)

    // Accents
    val Teal = Color(0xFF5EEAD4)
    val TealDim = Color(0xFF2DD4BF)
    val TealGlow = Color(0x245EEAD4)   // rgba(94,234,212,0.14)

    // Ink
    val Ink = Color(0xFFE6F2EF)
    val Ink2 = Color(0xFF8FA8A4)
    val Ink3 = Color(0xFF4F6663)
    val Ink4 = Color(0xFF2C3F3D)

    // Strokes
    val Hair = Color(0x125EEAD4)       // rgba(94,234,212,0.07)
    val Hair2 = Color(0x1F5EEAD4)      // rgba(94,234,212,0.12)

    // Reminder accents (also referenced via ui/tasks/ReminderStyle.kt)
    val Silent = Color(0xFF6B807D)
    val Notify = Color(0xFF5EEAD4)
    val Alarm = Color(0xFFFBBF24)

    // FAB / on-teal foreground
    val OnTeal = Color(0xFF0A1413)

    // Shape (the few we use repeatedly)
    val Radius = 14.dp
}
