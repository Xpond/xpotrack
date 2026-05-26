package com.xpotrack.app.ui.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect

// BasicTextField requests its caret rect be brought into view on tap/selection
// change. The framework's scroll container honours that by scrolling until the
// rect's top is at y=0 of the scroll viewport — which lands under our overlaid
// pinned header. We own caret-following via CaretScrollEffect (header-aware),
// so swallow the framework's request here.
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.swallowBringIntoView(): Modifier =
    bringIntoViewResponder(SwallowResponder)

@OptIn(ExperimentalFoundationApi::class)
private object SwallowResponder : BringIntoViewResponder {
    override fun calculateRectForParent(localRect: Rect): Rect = Rect.Zero
    override suspend fun bringChildIntoView(localRect: () -> Rect?) { /* swallow */ }
}
