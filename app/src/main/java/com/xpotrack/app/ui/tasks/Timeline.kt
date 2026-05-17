package com.xpotrack.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R
import com.xpotrack.app.ui.components.PillSize
import com.xpotrack.app.ui.components.XpReminderPill
import com.xpotrack.app.ui.components.styleFor
import com.xpotrack.app.ui.theme.XpTokens

private const val NowTime = "09:41"

@Composable
fun TimelineView(tasks: List<TaskRow>, modifier: Modifier = Modifier) {
    val totalHeight = ((TimelineEndHour - TimelineStartHour) * HourHeightDp + 40).dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
            .padding(top = 8.dp),
    ) {
        HourGrid()
        tasks.forEach { TaskPill(it) }
        NowIndicator()
    }
}

@Composable
private fun HourGrid() {
    for (i in 0..(TimelineEndHour - TimelineStartHour)) {
        val h = TimelineStartHour + i
        val y = (i * HourHeightDp).dp
        val label = formatHour(h)
        Text(
            label,
            modifier = Modifier
                .offset(x = 22.dp, y = y)
                .width(48.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.5.sp),
            color = XpTokens.Ink3,
        )
        Box(
            modifier = Modifier
                .offset(x = 76.dp, y = y + 6.dp)
                .padding(end = 22.dp)
                .fillMaxWidth()
                .height(0.5.dp)
                .background(XpTokens.Hair)
        )
    }
}

private fun formatHour(h: Int): String = when {
    h == 0 -> "12 AM"
    h == 12 -> "12 PM"
    h > 12 -> "${h - 12} PM"
    else -> "$h AM"
}

@Composable
private fun TaskPill(task: TaskRow) {
    val style = styleFor(task.level)
    val y = (timeToOffsetDp(task.time) - 4).dp
    val h = maxOf(36f, task.durationMin * MinHeightPx).dp
    Row(
        modifier = Modifier
            .offset(x = 78.dp, y = y)
            .padding(end = 18.dp)
            .fillMaxWidth()
            .height(h)
            .alpha(if (task.done) 0.45f else 1f),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(h)
                .clip(CircleShape)
                .background(style.accent.copy(alpha = if (task.done) 0.3f else 1f))
        )
        Spacer(Modifier.width(10.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (task.done) Color.Transparent else style.cardBg)
                .border(0.5.dp, if (task.done) Color.Transparent else XpTokens.Hair, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                XpReminderPill(task.level, task.time, PillSize.Sm)
                if (task.done) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = "Done",
                        tint = XpTokens.Ink3,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                task.label,
                style = TextStyle(
                    fontSize = 14.sp,
                    color = XpTokens.Ink,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                ),
            )
        }
    }
}

@Composable
private fun NowIndicator() {
    val y = timeToOffsetDp(NowTime).dp + 6.dp
    Row(
        modifier = Modifier
            .offset(x = 22.dp, y = y)
            .padding(end = 22.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "NOW",
            modifier = Modifier.width(48.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.5.sp),
            color = XpTokens.Teal,
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(XpTokens.Teal)
        )
        Box(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(XpTokens.Teal.copy(alpha = 0.6f))
        )
    }
}

