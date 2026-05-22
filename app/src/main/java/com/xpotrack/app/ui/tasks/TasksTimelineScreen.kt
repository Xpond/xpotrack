package com.xpotrack.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R
import com.xpotrack.app.ui.components.ConfirmDeleteDialog
import com.xpotrack.app.ui.components.DateTimeStrip
import com.xpotrack.app.ui.components.EmptyState
import com.xpotrack.app.ui.components.PinnedHeader
import com.xpotrack.app.ui.components.XpFab
import com.xpotrack.app.ui.theme.XpTokens
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun TasksTimelineScreen(
    tasks: List<TaskRow>,
    selectedDate: Long,
    datesWithTasks: Set<Long>,
    onSelectDate: (Long) -> Unit,
    onOpenTask: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onDeleteTask: (Long) -> Unit = {},
) {
    var pendingDelete by remember { mutableStateOf<TaskRow?>(null) }
    var calendarOpen by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var headerPx by remember { mutableIntStateOf(0) }
    val headerDp = with(density) { headerPx.toDp() }
    val today = remember { LocalDate.now(ZoneId.systemDefault()).toEpochDay() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(XpTokens.Bg),
    ) {
        TopHalo()
        if (tasks.isEmpty()) {
            val (title, helper) = when {
                selectedDate < today -> "No tasks that day" to "Past days stay as they were"
                selectedDate == today -> "Nothing scheduled today" to "Tap + to add the first"
                else -> "Nothing scheduled" to "Tap + to plan this day"
            }
            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(headerDp))
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    EmptyState(title, helper)
                }
            }
        } else Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(headerDp))
            TimelineView(
                tasks = tasks,
                onTaskTap = onOpenTask,
                onTaskLongPress = { pendingDelete = it },
            )
            Spacer(Modifier.height(120.dp))
        }
        PinnedHeader(onSize = { headerPx = it.height }) {
            TasksHeader(selectedDate = selectedDate, tasks = tasks)
            Row(verticalAlignment = Alignment.CenterVertically) {
                DayChipStrip(
                    selectedDate = selectedDate,
                    datesWithTasks = datesWithTasks,
                    onSelectDate = onSelectDate,
                    modifier = Modifier.weight(1f),
                )
                CalendarBtn(
                    onClick = { calendarOpen = true },
                    modifier = Modifier.padding(end = 14.dp),
                )
            }
        }
        if (selectedDate >= today) {
            XpFab(R.drawable.ic_plus, "New task", shadow = true, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 50.dp, bottom = 130.dp), onClick = { onOpenTask(0L) })
        }
    }
    pendingDelete?.let { task ->
        ConfirmDeleteDialog(
            title = "Delete task?",
            subject = task.label.ifBlank { "Untitled" },
            onCancel = { pendingDelete = null },
            onConfirm = {
                onDeleteTask(task.id)
                pendingDelete = null
            },
        )
    }
    if (calendarOpen) {
        MonthPickerDialog(
            selectedEpochDay = selectedDate,
            datesWithTasks = datesWithTasks,
            onPick = { picked -> onSelectDate(picked); calendarOpen = false },
            onDismiss = { calendarOpen = false },
        )
    }
}

@Composable
private fun CalendarBtn(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_calendar),
            contentDescription = "Pick date",
            tint = XpTokens.Ink2,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun TopHalo() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                Brush.radialGradient(
                    0f to XpTokens.TealGlow,
                    0.7f to Color.Transparent,
                )
            )
    )
}

@Composable
private fun TasksHeader(selectedDate: Long, tasks: List<TaskRow>) {
    val done = tasks.count { it.done }
    val total = tasks.size
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 18.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            DateTimeStrip()
            Spacer(Modifier.height(14.dp))
            Text(
                dayOfWeekTitle(selectedDate),
                style = MaterialTheme.typography.displayLarge,
                color = XpTokens.Ink,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = XpTokens.Teal)) { append("$done") }
                    withStyle(SpanStyle(color = XpTokens.Ink3)) { append("/$total") }
                },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 26.sp),
            )
            Spacer(Modifier.height(4.dp))
            Text("done".uppercase(), style = MaterialTheme.typography.labelMedium, color = XpTokens.Ink3)
        }
    }
}


