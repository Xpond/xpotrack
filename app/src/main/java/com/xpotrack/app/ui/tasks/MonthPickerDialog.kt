package com.xpotrack.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R
import com.xpotrack.app.ui.theme.XpTokens
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

// Minimal month grid. Tap a day → selects it and closes. Chevrons page months.
// Past months are reachable but past days within the current month are dimmed
// (still selectable — historic tasks are allowed).
@Composable
fun MonthPickerDialog(
    selectedEpochDay: Long,
    datesWithTasks: Set<Long> = emptySet(),
    // When true (create-task flow), past days render as inert + dimmed since
    // scheduling a reminder in the past is meaningless. The timeline browser
    // leaves this false so users can revisit historical days.
    disablePast: Boolean = false,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = remember { LocalDate.now(ZoneId.systemDefault()) }
    val selected = remember(selectedEpochDay) { LocalDate.ofEpochDay(selectedEpochDay) }
    var month by remember { mutableStateOf(YearMonth.from(selected)) }

    DialogCard(onDismiss) {
        Column {
            MonthHeader(
                month = month,
                onPrev = { month = month.minusMonths(1) },
                onNext = { month = month.plusMonths(1) },
            )
            Spacer(Modifier.size(14.dp))
            WeekdayRow()
            Spacer(Modifier.size(6.dp))
            MonthGrid(
                month = month,
                selected = selected,
                today = today,
                datesWithTasks = datesWithTasks,
                disablePast = disablePast,
                onPick = { d -> onPick(d.toEpochDay()) },
            )
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChevronBtn(R.drawable.ic_chevron_left, "Previous month", onPrev)
        Spacer(Modifier.size(8.dp))
        Text(
            "%s %d".format(
                month.month.getDisplayName(JTextStyle.FULL, Locale.getDefault()),
                month.year,
            ),
            modifier = Modifier.weight(1f),
            color = XpTokens.Ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        ChevronBtn(R.drawable.ic_chevron_right, "Next month", onNext)
    }
}

@Composable
private fun ChevronBtn(iconRes: Int, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(iconRes), desc, tint = XpTokens.Ink2, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun WeekdayRow() {
    val firstDow = DayOfWeek.MONDAY
    val labels = (0 until 7).map { i ->
        firstDow.plus(i.toLong()).getDisplayName(JTextStyle.NARROW, Locale.getDefault())
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        labels.forEach { l ->
            Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                Text(l, color = XpTokens.Ink3, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    today: LocalDate,
    datesWithTasks: Set<Long>,
    disablePast: Boolean,
    onPick: (LocalDate) -> Unit,
) {
    val first = month.atDay(1)
    // Leading blanks so column 0 = Monday.
    val leading = ((first.dayOfWeek.value - DayOfWeek.MONDAY.value) + 7) % 7
    val daysInMonth = month.lengthOfMonth()
    val cells = leading + daysInMonth
    val rows = (cells + 6) / 7
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (c in 0 until 7) {
                    val idx = r * 7 + c
                    val dayNum = idx - leading + 1
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (dayNum in 1..daysInMonth) {
                            val d = month.atDay(dayNum)
                            val isPast = d.isBefore(today)
                            DayCell(
                                day = dayNum,
                                isSelected = d == selected,
                                isToday = d == today,
                                isPast = isPast,
                                isDisabled = disablePast && isPast,
                                hasTask = d.toEpochDay() in datesWithTasks,
                                onClick = { if (!(disablePast && isPast)) onPick(d) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    isPast: Boolean,
    isDisabled: Boolean,
    hasTask: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (isSelected) XpTokens.Teal else Color.Transparent
    val borderColor = if (isToday && !isSelected) XpTokens.Teal else Color.Transparent
    val fg = when {
        isSelected -> XpTokens.OnTeal
        isToday -> XpTokens.Teal
        isPast -> XpTokens.Ink3
        else -> XpTokens.Ink
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bg)
            .border(0.5.dp, borderColor, CircleShape)
            .clickable(enabled = !isDisabled, onClick = onClick)
            .alpha(if (isDisabled) 0.35f else 1f),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                day.toString(),
                color = fg,
                fontSize = 13.sp,
                fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
            )
            Box(
                Modifier
                    .size(3.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasTask) (if (isSelected) XpTokens.OnTeal else XpTokens.Teal)
                        else Color.Transparent
                    )
            )
        }
    }
}
