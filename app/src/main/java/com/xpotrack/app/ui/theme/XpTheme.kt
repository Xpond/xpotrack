package com.xpotrack.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val XpColorScheme = darkColorScheme(
    primary = XpTokens.Teal,
    onPrimary = XpTokens.OnTeal,
    secondary = XpTokens.TealDim,
    background = XpTokens.Bg,
    onBackground = XpTokens.Ink,
    surface = XpTokens.Surface1,
    onSurface = XpTokens.Ink,
    surfaceVariant = XpTokens.Surface2,
    onSurfaceVariant = XpTokens.Ink2,
    error = XpTokens.Alarm,
)

@Composable
fun XpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = XpColorScheme,
        typography = XpTypography,
        content = content,
    )
}
