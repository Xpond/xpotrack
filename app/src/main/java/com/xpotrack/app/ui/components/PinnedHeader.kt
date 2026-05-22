package com.xpotrack.app.ui.components

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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.xpotrack.app.ui.theme.XpTokens

// Pad to the full status bar height (regardless of visibility) so when the
// bar swipes in transiently, its bottom edge sits flush against content.
// displayCutout alone would only clear the notch (~22dp) but the system bar
// is taller (~28dp), leaving a visible seam below the bar when revealed.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Modifier.cutoutSafeTopPadding(): Modifier =
    padding(WindowInsets.statusBarsIgnoringVisibility.asPaddingValues())

// Header that overlays a scrolling list: 90% Bg fill that fades to transparent
// over the last ~24dp so its bottom edge dissolves into the content beneath
// instead of cutting a hard seam. Tap-absorber so empty zones don't fire row
// taps below. Size callback lets the list reserve matching top padding. Top
// inset matches the full status bar height (see cutoutSafeTopPadding) so the
// bar's bottom edge sits flush against content when swiped in.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PinnedHeader(
    onSize: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val tint = XpTokens.Bg.copy(alpha = 0.9f)
    val fadePx = with(LocalDensity.current) { 24.dp.toPx() }
    Column(
        modifier
            .fillMaxWidth()
            .onSizeChanged(onSize)
            .pointerInput(Unit) { detectTapGestures { } }
            .drawBehind {
                val solidEnd = (size.height - fadePx).coerceAtLeast(0f)
                drawRect(color = tint, size = Size(size.width, solidEnd))
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to tint,
                        1f to Color.Transparent,
                        startY = solidEnd,
                        endY = size.height,
                    ),
                    topLeft = Offset(0f, solidEnd),
                    size = Size(size.width, fadePx),
                )
            }
            .padding(WindowInsets.statusBarsIgnoringVisibility.asPaddingValues())
    ) {
        content()
    }
}
