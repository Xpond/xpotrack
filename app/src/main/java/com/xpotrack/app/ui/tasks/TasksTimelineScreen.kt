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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R
import com.xpotrack.app.ui.components.ConfirmDeleteDialog
import com.xpotrack.app.ui.components.DateTimeStrip
import com.xpotrack.app.ui.theme.XpTokens
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TasksTimelineScreen(
    tasks: List<TaskRow>,
    selectedDate: Long,
    datesWithTasks: Set<Long>,
    onSelectDate: (Long) -> Unit,
    onOpenTask: (Long) -> Unit,
    onDeleteTask: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<TaskRow?>(null) }
    var calendarOpen by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(XpTokens.Bg),
    ) {
        TopHalo()
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
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
            TimelineView(
                tasks = tasks,
                onTaskTap = onOpenTask,
                onTaskLongPress = { pendingDelete = it },
            )
            Spacer(Modifier.height(120.dp))
        }
        val today = remember { LocalDate.now(ZoneId.systemDefault()).toEpochDay() }
        if (selectedDate >= today) {
            TasksFab(Modifier.align(Alignment.BottomEnd), onClick = { onOpenTask(0L) })
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
            .padding(start = 22.dp, end = 18.dp, top = 14.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            DateTimeStrip()
            Spacer(Modifier.height(14.dp))
            Text(
                titleFor(selectedDate),
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

// "Today" only for today. Tomorrow/Yesterday for ±1 day. Otherwise weekday
// name (Monday, Tuesday, …) — date context already shown in DateTimeStrip
// above and in the day chips below.
private fun titleFor(selectedEpochDay: Long): String {
    val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
    return when (selectedEpochDay - today) {
        0L -> "Today"
        1L -> "Tomorrow"
        -1L -> "Yesterday"
        else -> LocalDate.ofEpochDay(selectedEpochDay)
            .dayOfWeek
            .getDisplayName(TextStyle.FULL, Locale.getDefault())
    }
}

@Composable
private fun TasksFab(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .padding(end = 22.dp, bottom = 86.dp)
            .size(56.dp)
            .shadow(elevation = 18.dp, shape = CircleShape, ambientColor = XpTokens.Teal, spotColor = XpTokens.Teal)
            .clip(CircleShape)
            .background(XpTokens.Teal)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = "New task",
            tint = XpTokens.OnTeal,
            modifier = Modifier.size(22.dp),
        )
    }
}
