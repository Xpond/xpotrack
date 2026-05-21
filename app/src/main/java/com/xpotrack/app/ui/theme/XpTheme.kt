package com.xpotrack.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun XpTheme(content: @Composable () -> Unit) {
    // Reads from the live XpTokens, so the MaterialTheme palette also flips
    // when the user toggles light/dark in Settings. The `Ink == Dark.Ink` check
    // is how we decide which colorScheme constructor to use — light vs dark
    // affects defaults on Material3 components we don't override.
    val isDark = XpTokens.Ink == Dark.Ink
    val scheme = if (isDark) darkColorScheme(
        primary = XpTokens.Teal, onPrimary = XpTokens.OnTeal,
        secondary = XpTokens.TealDim,
        background = XpTokens.Bg, onBackground = XpTokens.Ink,
        surface = XpTokens.Surface1, onSurface = XpTokens.Ink,
        surfaceVariant = XpTokens.Surface2, onSurfaceVariant = XpTokens.Ink2,
        error = XpTokens.Alarm,
    ) else lightColorScheme(
        primary = XpTokens.Teal, onPrimary = XpTokens.OnTeal,
        secondary = XpTokens.TealDim,
        background = XpTokens.Bg, onBackground = XpTokens.Ink,
        surface = XpTokens.Surface1, onSurface = XpTokens.Ink,
        surfaceVariant = XpTokens.Surface2, onSurfaceVariant = XpTokens.Ink2,
        error = XpTokens.Alarm,
    )
    MaterialTheme(colorScheme = scheme, typography = XpTypography, content = content)
}
