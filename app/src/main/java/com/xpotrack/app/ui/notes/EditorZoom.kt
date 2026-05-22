package com.xpotrack.app.ui.notes

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.xpotrack.app.data.prefs.EditorZoomPrefs

// Scales every sp inside `content` by `zoom` via LocalDensity.fontScale. One
// override and every Text/TextField in the subtree scales uniformly — no need
// to touch the markdown renderer's hardcoded sizes.
@Composable
fun ZoomedText(zoom: Float, content: @Composable () -> Unit) {
    val base = LocalDensity.current
    val scaled = remember(base, zoom) {
        Density(density = base.density, fontScale = base.fontScale * zoom)
    }
    CompositionLocalProvider(LocalDensity provides scaled, content = content)
}

// Two-finger pinch to scale `zoom`. Runs in the Initial pass so we see events
// before verticalScroll/text selection do. Once a second finger lands we claim
// the gesture by consuming every event — including any backlog from the first
// finger — so scroll doesn't pick up drift while the pinch is happening.
fun Modifier.pinchZoom(zoom: Float, onZoom: (Float) -> Unit): Modifier =
    pointerInput(Unit) {
        var current = zoom
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var zooming = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pressedCount = event.changes.count { it.pressed }
                if (pressedCount >= 2) {
                    val change = event.calculateZoom()
                    if (change != 1f) {
                        current = (current * change).coerceIn(EditorZoomPrefs.MIN, EditorZoomPrefs.MAX)
                        onZoom(current)
                    }
                    zooming = true
                }
                if (zooming) event.changes.forEach { if (it.pressed) it.consume() }
            } while (event.changes.any { it.pressed })
        }
    }
