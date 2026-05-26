package com.xpotrack.app.ui.notes

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextLayoutResult

// We own caret-follow scrolling for the notes/quick/vault editors. The framework's
// BasicTextField bringIntoView is swallowed at the field (see SwallowBringIntoView)
// because it scrolls until the caret is at y=0 of the scroll viewport — under our
// overlaid PinnedHeader. Here we know about both insets and place the caret in the
// gap between them.
//
// We hold the TextLayoutResult, not a Rect: BasicTextField fires onTextLayout only
// on *text* changes, not selection-only changes. Looking up the caret rect from
// the current layout on every effect run means a tap on line 1 uses line 1's
// coordinates, not the stale rect from wherever the caret was before.
class CaretScrollState(private val scroll: ScrollState) {
    var viewportHeightPx by mutableIntStateOf(0)
    var topInsetPx by mutableIntStateOf(0)
    var bottomInsetPx by mutableIntStateOf(0)
    var fieldTopInScrollPx by mutableIntStateOf(0)
    var layout by mutableStateOf<TextLayoutResult?>(null)

    suspend fun bringCaretIntoView(caretOffset: Int) {
        val l = layout ?: return
        if (viewportHeightPx == 0) return
        val rect = l.getCursorRect(caretOffset.coerceIn(0, l.layoutInput.text.length))
        val caretTopInViewport = fieldTopInScrollPx + rect.top.toInt() - scroll.value
        val caretBottomInViewport = fieldTopInScrollPx + rect.bottom.toInt() - scroll.value
        val bottomOverflow = caretBottomInViewport - (viewportHeightPx - bottomInsetPx - CARET_BOTTOM_MARGIN_PX)
        val topOverflow = (topInsetPx + CARET_TOP_MARGIN_PX) - caretTopInViewport
        val delta = when {
            bottomOverflow > 0 -> bottomOverflow
            topOverflow > 0 -> -topOverflow
            else -> 0
        }
        if (delta != 0) {
            scroll.animateScrollTo((scroll.value + delta).coerceIn(0, scroll.maxValue))
        }
    }
}

private const val CARET_BOTTOM_MARGIN_PX = 80
private const val CARET_TOP_MARGIN_PX = 24

@Composable
fun rememberCaretScroll(scroll: ScrollState): CaretScrollState =
    remember(scroll) { CaretScrollState(scroll) }

@Composable
fun CaretScrollEffect(state: CaretScrollState, caretOffset: Int) {
    LaunchedEffect(
        caretOffset, state.layout, state.viewportHeightPx,
        state.topInsetPx, state.bottomInsetPx,
    ) {
        state.bringCaretIntoView(caretOffset)
    }
}
