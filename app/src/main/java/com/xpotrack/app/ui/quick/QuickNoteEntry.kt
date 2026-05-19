package com.xpotrack.app.ui.quick

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.ui.theme.XpTokens

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickNoteEntry(
    row: QuickRow,
    isLast: Boolean,
    onKeep: () -> Unit,
    onOpen: () -> Unit = {},
    onLongPress: () -> Unit = {},
) {
    val accent = if (row.expiring) XpTokens.Alarm else XpTokens.TealDim
    val chipBg = if (row.expiring) Color(0x1AFBBF24) else Color(0x0F5EEAD4)
    val chipBorder = if (row.expiring) Color(0x4DFBBF24) else XpTokens.Hair2
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (row.pct < 15) 0.7f else 1f)
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress)
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            row.text.lineSequence().firstOrNull()?.trim().orEmpty().ifEmpty { "Untitled" },
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
            color = XpTokens.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        CountdownChip(pct = row.pct, label = row.leftLabel, accent = accent, bg = chipBg, border = chipBorder)
        Spacer(Modifier.width(10.dp))
        Text(
            "Keep".uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = XpTokens.Ink3,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onKeep)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
    if (!isLast) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .height(0.5.dp)
                .background(XpTokens.Hair)
        )
    }
}

@Composable
private fun CountdownChip(pct: Int, label: String, accent: Color, bg: Color, border: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .border(0.5.dp, border, CircleShape)
            .padding(horizontal = 10.dp, vertical = 2.dp),
    ) {
        ProgressRing(pct = pct, accent = accent)
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
            color = accent,
        )
    }
}

@Composable
private fun ProgressRing(pct: Int, accent: Color) {
    Canvas(modifier = Modifier.size(11.dp)) {
        val stroke = 1.5.dp.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)
        drawArc(
            color = accent.copy(alpha = 0.25f),
            startAngle = 0f, sweepAngle = 360f, useCenter = false,
            topLeft = topLeft, size = arcSize,
            style = Stroke(width = stroke),
        )
        drawArc(
            color = accent,
            startAngle = -90f, sweepAngle = 360f * (pct / 100f), useCenter = false,
            topLeft = topLeft, size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}
