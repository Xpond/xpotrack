package com.xpotrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.xpotrack.app.ui.theme.XpTokens

// Pad to the full status bar height (regardless of visibility) so when the
// bar swipes in transiently, its bottom edge sits flush against content.
// displayCutout alone would only clear the notch (~22dp) but the system bar
// is taller (~28dp), leaving a visible seam below the bar when revealed.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Modifier.cutoutSafeTopPadding(): Modifier =
    padding(WindowInsets.statusBarsIgnoringVisibility.asPaddingValues())

// Header that overlays a scrolling list: 90% Bg fill so content faintly shows
// through, a tap-absorber so empty zones don't fire row taps below, and a
// size callback so the list can reserve matching top padding. Top inset
// matches the full status bar height (see cutoutSafeTopPadding) so the bar's
// bottom edge sits flush against content when swiped in.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PinnedHeader(
    onSize: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .onSizeChanged(onSize)
            .pointerInput(Unit) { detectTapGestures { } }
            .background(XpTokens.Bg.copy(alpha = 0.9f))
            .padding(WindowInsets.statusBarsIgnoringVisibility.asPaddingValues())
    ) {
        content()
    }
}
