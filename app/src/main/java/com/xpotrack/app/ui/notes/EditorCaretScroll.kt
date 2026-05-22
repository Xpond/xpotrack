package com.xpotrack.app.ui.notes

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect

// Compose 1.7 BasicTextField + verticalScroll: bringIntoView doesn't fire for
// caret-only changes (Enter at end of text), so the caret slides under the
// keyboard with no scroll response. We track the caret's Y inside the scroll
// viewport ourselves and animate scroll when it nears the bottom.
class CaretScrollState(private val scroll: ScrollState) {
    var viewportHeightPx by mutableStateOf(0)
    var fieldTopInScrollPx by mutableStateOf(0)
    var caretRect by mutableStateOf<Rect?>(null)

    suspend fun bringCaretIntoView() {
        val rect = caretRect ?: return
        if (viewportHeightPx == 0) return
        val caretBottomInViewport = fieldTopInScrollPx + rect.bottom.toInt() - scroll.value
        val overflow = caretBottomInViewport - (viewportHeightPx - CARET_BOTTOM_MARGIN_PX)
        if (overflow > 0) {
            scroll.animateScrollTo((scroll.value + overflow).coerceAtMost(scroll.maxValue))
        }
    }
}

private const val CARET_BOTTOM_MARGIN_PX = 80

@Composable
fun rememberCaretScroll(scroll: ScrollState): CaretScrollState =
    remember(scroll) { CaretScrollState(scroll) }

@Composable
fun CaretScrollEffect(state: CaretScrollState, selectionKey: Any) {
    LaunchedEffect(selectionKey, state.caretRect, state.viewportHeightPx) {
        state.bringCaretIntoView()
    }
}
