package com.xpotrack.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xpotrack.app.ui.theme.XpTokens

// Hand-rolled spinner — Material's CircularProgressIndicator gets throttled
// to a static frame on some Vivo/Funtouch builds. withFrameMillis is tied to
// the Choreographer and can't be disabled by power-saving heuristics.
@Composable
fun FrameSpinner(size: Dp = 16.dp, stroke: Dp = 2.dp) {
    var angle by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameMillis { now ->
                if (last != 0L) angle = (angle + (now - last) * 0.36f) % 360f
                last = now
            }
        }
    }
    Canvas(Modifier.size(size)) {
        val strokePx = stroke.toPx()
        val inset = strokePx / 2f
        drawArc(
            color = XpTokens.Teal,
            startAngle = angle,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(this.size.width - strokePx, this.size.height - strokePx),
            style = Stroke(width = strokePx),
        )
    }
}
