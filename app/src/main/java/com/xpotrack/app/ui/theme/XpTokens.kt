package com.xpotrack.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Mirrors misc/mockups/screens/system.jsx for the dark palette. Fields are
// `var ... by mutableStateOf(...)` so the Settings light/dark toggle can swap
// the live palette in place and every call site recomposes automatically.
// Trade-off: this is a global mutable singleton, so there's only one active
// palette at a time and @Preview composables render with whatever was applied
// last. Acceptable for a single-toggle theme in this app.
object XpTokens {
    var Bg by mutableStateOf(Dark.Bg)
    var Surface1 by mutableStateOf(Dark.Surface1)
    var Surface2 by mutableStateOf(Dark.Surface2)

    var Teal by mutableStateOf(Dark.Teal)
    var TealDim by mutableStateOf(Dark.TealDim)
    var TealGlow by mutableStateOf(Dark.TealGlow)

    var Ink by mutableStateOf(Dark.Ink)
    var Ink2 by mutableStateOf(Dark.Ink2)
    var Ink3 by mutableStateOf(Dark.Ink3)
    var Ink4 by mutableStateOf(Dark.Ink4)

    var Hair by mutableStateOf(Dark.Hair)
    var Hair2 by mutableStateOf(Dark.Hair2)

    var Silent by mutableStateOf(Dark.Silent)
    var Notify by mutableStateOf(Dark.Notify)
    var Alarm by mutableStateOf(Dark.Alarm)

    var OnTeal by mutableStateOf(Dark.OnTeal)

    // TealTint is the ~6% teal chip background used on circular icon backdrops
    // in the vault (lock icons, fingerprint badge inner disc).
    var TealTint by mutableStateOf(Dark.TealTint)

    val Radius = 14.dp

    fun apply(p: XpPalette) {
        Bg = p.Bg; Surface1 = p.Surface1; Surface2 = p.Surface2
        Teal = p.Teal; TealDim = p.TealDim; TealGlow = p.TealGlow
        Ink = p.Ink; Ink2 = p.Ink2; Ink3 = p.Ink3; Ink4 = p.Ink4
        Hair = p.Hair; Hair2 = p.Hair2
        Silent = p.Silent; Notify = p.Notify; Alarm = p.Alarm
        OnTeal = p.OnTeal
        TealTint = p.TealTint
    }
}

data class XpPalette(
    val Bg: Color, val Surface1: Color, val Surface2: Color,
    val Teal: Color, val TealDim: Color, val TealGlow: Color,
    val Ink: Color, val Ink2: Color, val Ink3: Color, val Ink4: Color,
    val Hair: Color, val Hair2: Color,
    val Silent: Color, val Notify: Color, val Alarm: Color,
    val OnTeal: Color,
    val TealTint: Color,
)

val Dark = XpPalette(
    Bg = Color(0xFF06100F),
    Surface1 = Color(0xFF0C1A19),
    Surface2 = Color(0xFF122524),
    Teal = Color(0xFF5EEAD4),
    TealDim = Color(0xFF2DD4BF),
    TealGlow = Color(0x245EEAD4),
    Ink = Color(0xFFE6F2EF),
    Ink2 = Color(0xFF8FA8A4),
    Ink3 = Color(0xFF4F6663),
    Ink4 = Color(0xFF2C3F3D),
    Hair = Color(0x125EEAD4),
    Hair2 = Color(0x1F5EEAD4),
    Silent = Color(0xFF6B807D),
    Notify = Color(0xFF5EEAD4),
    Alarm = Color(0xFFFBBF24),
    OnTeal = Color(0xFF0A1413),
    TealTint = Color(0x0F5EEAD4),
)

// Light palette: inverted surfaces + ink, teal accent darkened for contrast on
// pale surfaces. Same teal-tinted hairlines so the visual language survives.
val Light = XpPalette(
    Bg = Color(0xFFF6FBFA),
    Surface1 = Color(0xFFFFFFFF),
    Surface2 = Color(0xFFECF4F3),
    Teal = Color(0xFF0D9488),
    TealDim = Color(0xFF14B8A6),
    TealGlow = Color(0x140D9488),
    Ink = Color(0xFF06100F),
    Ink2 = Color(0xFF3F5754),
    Ink3 = Color(0xFF6B8481),
    Ink4 = Color(0xFFA8BCB9),
    Hair = Color(0x1F0D9488),
    Hair2 = Color(0x330D9488),
    Silent = Color(0xFF6B807D),
    Notify = Color(0xFF0D9488),
    Alarm = Color(0xFFCA8A04),
    OnTeal = Color(0xFFFFFFFF),
    TealTint = Color(0x140D9488),
)
