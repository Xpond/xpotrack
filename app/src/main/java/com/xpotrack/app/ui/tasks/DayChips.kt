package com.xpotrack.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.ui.theme.XpTokens
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

// Horizontally scrollable strip of every day in the selected day's month.
// Today owns the teal highlight; the user's selected day gets a subtler
// underline so the scroll position still tells them what they tapped.
// A small dot under the date marks days that have at least one task.
@Composable
fun DayChipStrip(
    selectedDate: Long,
    datesWithTasks: Set<Long>,
    onSelectDate: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now(ZoneId.systemDefault()).toEpochDay() }
    val selected = remember(selectedDate) { LocalDate.ofEpochDay(selectedDate) }
    // Window of ±6 months around the selected day. Wide enough that scrolling
    // feels continuous; cheap because each chip is tiny. We rebuild the
    // window when the user jumps to a date outside it (e.g. via the month
    // picker), keeping it centered on whatever they just chose.
    val (windowStart, days) = remember(selected) {
        val start = selected.minusMonths(6).withDayOfMonth(1)
        val end = selected.plusMonths(6).let { it.withDayOfMonth(it.lengthOfMonth()) }
        val list = generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .map { it.toEpochDay() }
            .toList()
        start.toEpochDay() to list
    }

    val selectedIndex = (selectedDate - windowStart).toInt()
    // Start the list already positioned on the selected day. Without this,
    // every entry to the screen animates from index 0 → selected, which
    // reads as a jitter on the chip strip.
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceAtLeast(0),
    )
    LaunchedEffect(selectedDate, windowStart) {
        if (selectedIndex !in days.indices) return@LaunchedEffect
        // Only scroll when the selected chip isn't already on screen — i.e.
        // an external jump (month picker, fresh window). Tapping a visible
        // chip must not shift the strip, or it reads as jitter.
        val visible = listState.layoutInfo.visibleItemsInfo
        val onScreen = visible.any { it.index == selectedIndex }
        if (!onScreen) {
            listState.animateScrollToItem(selectedIndex, scrollOffset = -120)
        }
    }

    // Month label updates as the user scrolls. We pick the leftmost item
    // whose left edge has actually scrolled into view (offset >= 0) so the
    // label flips at the exact moment the month rolls over, not while the
    // last day of the previous month is still half-visible.
    val visibleMonth by remember(days) {
        derivedStateOf {
            val info = listState.layoutInfo.visibleItemsInfo
            val anchor = info.firstOrNull { it.offset >= 0 } ?: info.firstOrNull()
            val idx = anchor?.index ?: selectedIndex
            val epochDay = days.getOrNull(idx) ?: selectedDate
            YearMonth.from(LocalDate.ofEpochDay(epochDay))
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "%s %d".format(
                visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                visibleMonth.year,
            ).uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
            color = XpTokens.Ink3,
            modifier = Modifier.padding(start = 22.dp, top = 10.dp),
        )
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
            contentPadding = PaddingValues(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(days, key = { it }) { epochDay ->
                val date = LocalDate.ofEpochDay(epochDay)
                DayChipBox(
                    label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    dayNum = date.dayOfMonth.toString(),
                    isToday = epochDay == today,
                    isSelected = epochDay == selectedDate,
                    isPast = epochDay < today,
                    hasTask = epochDay in datesWithTasks,
                    onClick = { onSelectDate(epochDay) },
                )
            }
        }
    }
}

@Composable
private fun DayChipBox(
    label: String,
    dayNum: String,
    isToday: Boolean,
    isSelected: Boolean,
    isPast: Boolean,
    hasTask: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (isToday) XpTokens.Teal else Color.Transparent
    val bg = if (isToday) Color(0x1A5EEAD4) else Color.Transparent
    val labelColor = if (isToday) XpTokens.Teal else XpTokens.Ink3
    val dateColor = when {
        isToday -> XpTokens.Teal
        isSelected -> XpTokens.Ink
        else -> XpTokens.Ink2
    }
    Column(
        modifier = Modifier
            .width(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .background(bg)
            .border(0.5.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp)
            .alpha(if (isPast && !isToday) 0.45f else 1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.5.sp),
            color = labelColor,
        )
        Text(
            dayNum,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 16.sp),
            color = dateColor,
        )
        // Selection underline + task dot share this row; both small enough
        // to coexist without making the chip feel busy.
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .height(3.dp)
                .width(if (isSelected) 14.dp else 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isSelected -> Box(
                    Modifier
                        .height(2.dp)
                        .width(14.dp)
                        .clip(CircleShape)
                        .background(if (isToday) XpTokens.Teal else XpTokens.Ink2)
                )
                hasTask -> Box(
                    Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(XpTokens.Teal)
                )
            }
        }
    }
}
