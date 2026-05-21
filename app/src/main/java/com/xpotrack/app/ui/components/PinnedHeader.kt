package com.xpotrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.xpotrack.app.ui.theme.XpTokens

// Header that overlays a scrolling list: 90% Bg fill so content faintly shows
// through, a tap-absorber so empty zones don't fire row taps below, and a
// size callback so the list can reserve matching top padding.
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
    ) {
        content()
    }
}
