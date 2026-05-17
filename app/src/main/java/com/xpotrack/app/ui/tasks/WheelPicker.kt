package com.xpotrack.app.ui.tasks

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R
import com.xpotrack.app.ui.theme.XpTokens

val WheelItemHeight = 48.dp
private const val WheelVisibleItems = 3                   // center + 1 above + 1 below
val WheelHeight = WheelItemHeight * WheelVisibleItems
private const val LoopSpan = 10_000                       // half-window; total slots = 2 * LoopSpan
val WheelMonoFamily = FontFamily(Font(R.font.geist_mono_regular))

/**
 * Infinite-loop snap-to-item wheel. Renders ~20,000 virtual slots, mapping
 * each slot back to `values[slot % values.size]` so scroll wraps in both directions.
 * Emits `onIndexChange(valueIndex)` once a fling settles on a new center.
 */
@Composable
fun <T> WheelPicker(
    values: List<T>,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    width: Dp,
    format: (T) -> String,
    bigFontSize: TextUnit = 28.sp,
    smallFontSize: TextUnit = 18.sp,
) {
    val size = values.size
    // Viewport shows slots [first, first+1, first+2]; center slot = first+1.
    // We want (first + 1) mod size == selectedIndex → first = base + selectedIndex - 1.
    val base = remember(size) { (LoopSpan / size) * size }
    val initialFirst = remember(size, base) { (base + selectedIndex - 1).coerceAtLeast(0) }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialFirst)
    val fling = rememberSnapFlingBehavior(lazyListState = state)

    // Pull external state changes (e.g. AM/PM swap rewriting hour) into the wheel
    // only when the user isn't actively scrolling.
    LaunchedEffect(selectedIndex, size) {
        if (state.isScrollInProgress) return@LaunchedEffect
        val currentCenter = (state.firstVisibleItemIndex + 1).mod(size)
        if (currentCenter != selectedIndex) {
            val nearest = nearestSlotFor(state.firstVisibleItemIndex + 1, selectedIndex, size)
            state.scrollToItem((nearest - 1).coerceAtLeast(0))
        }
    }

    // Emit index changes once scroll settles on a new center slot.
    LaunchedEffect(state, size) {
        snapshotFlow { state.isScrollInProgress to state.firstVisibleItemIndex }
            .collect { (scrolling, first) ->
                if (scrolling) return@collect
                val v = (first + 1).mod(size)
                if (v != selectedIndex) onIndexChange(v)
            }
    }

    // Centered slot (closest to viewport center) → value index for highlighting.
    val centerValueIndex by remember(state, size) {
        derivedStateOf {
            val info = state.layoutInfo
            val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
            val slot = info.visibleItemsInfo.minByOrNull {
                kotlin.math.abs((it.offset + it.size / 2) - viewportCenter)
            }?.index ?: (state.firstVisibleItemIndex + 1)
            slot.mod(size)
        }
    }

    // Stop the parent ModalBottomSheet from treating our overflow as drag-to-dismiss.
    val swallowDrag = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
                Offset(0f, available.y)
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                Velocity(0f, available.y)
        }
    }

    LazyColumn(
        state = state,
        flingBehavior = fling,
        modifier = Modifier
            .width(width)
            .height(WheelHeight)
            .nestedScroll(swallowDrag),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(LoopSpan * 2) { slot ->
            val valueIdx = slot.mod(size)
            val isCenter = valueIdx == centerValueIndex
            Box(
                Modifier.height(WheelItemHeight).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    format(values[valueIdx]),
                    style = TextStyle(
                        fontSize = if (isCenter) bigFontSize else smallFontSize,
                        fontWeight = if (isCenter) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isCenter) XpTokens.Teal else XpTokens.Ink3.copy(alpha = 0.55f),
                        fontFamily = WheelMonoFamily,
                    ),
                )
            }
        }
    }
}

// Closest wrap of `target` to `currentSlot` — so external state changes don't
// jump the user far across the loop boundary.
private fun nearestSlotFor(currentSlot: Int, target: Int, size: Int): Int {
    val mod = currentSlot.mod(size)
    val diff = ((target - mod + size) % size).let { d -> if (d > size / 2) d - size else d }
    return currentSlot + diff
}
