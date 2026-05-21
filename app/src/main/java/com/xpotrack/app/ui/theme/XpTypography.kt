package com.xpotrack.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R

// Single-font experiment: everything maps to GeistMono. Only two physical
// weights ship (Regular, Medium), so SemiBold/Bold synthesize to Medium —
// Compose will fake-bold past that, which reads worse than just stopping.
val Geist = FontFamily(
    Font(R.font.geist_mono_regular, FontWeight.Normal),
    Font(R.font.geist_mono_medium, FontWeight.Medium),
    Font(R.font.geist_mono_medium, FontWeight.SemiBold),
    Font(R.font.geist_mono_medium, FontWeight.Bold),
)

val GeistMono = Geist

val XpTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Geist, fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp, letterSpacing = (-0.025).em, lineHeight = 33.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Geist, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, letterSpacing = (-0.015).em, lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Geist, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, letterSpacing = (-0.005).em, lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Geist, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, letterSpacing = (-0.005).em, lineHeight = 23.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = GeistMono, fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp, letterSpacing = 0.08.em,
    ),
    labelMedium = TextStyle(
        fontFamily = GeistMono, fontWeight = FontWeight.Medium,
        fontSize = 10.sp, letterSpacing = 0.06.em,
    ),
)
