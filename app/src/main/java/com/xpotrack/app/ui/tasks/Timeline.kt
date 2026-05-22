package com.xpotrack.app.ui.tasks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R
import com.xpotrack.app.ui.components.PillSize
import com.xpotrack.app.ui.components.XpReminderPill
import com.xpotrack.app.ui.components.styleFor
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun TimelineView(
    tasks: List<TaskRow>,
    modifier: Modifier = Modifier,
    onTaskTap: (Long) -> Unit = {},
    onTaskLongPress: (TaskRow) -> Unit = {},
) {
    val sorted = tasks.sortedBy {
        val (h, m) = parseHHmm(it.time); h * 60 + m
    }
    Column(modifier = modifier.padding(start = 22.dp, end = 22.dp, top = 12.dp)) {
        sorted.forEachIndexed { i, task ->
            TaskRowItem(
                task = task,
                onClick = { onTaskTap(task.id) },
                onLongClick = { onTaskLongPress(task) },
            )
            if (i < sorted.lastIndex) Spacer(Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRowItem(task: TaskRow, onClick: () -> Unit, onLongClick: () -> Unit) {
    val style = styleFor(task.level)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (task.done) 0.45f else 1f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(44.dp)
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
                XpReminderPill(task.level, formatTime12(task.time), PillSize.Sm)
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
                fontSize = 14.sp,
                color = XpTokens.Ink,
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
            )
        }
    }
}
