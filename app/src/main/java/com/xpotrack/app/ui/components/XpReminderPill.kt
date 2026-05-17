package com.xpotrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.data.model.ReminderLevel

enum class PillSize { Sm, Md }

@Composable
fun XpReminderPill(level: ReminderLevel, time: String, size: PillSize = PillSize.Md) {
    val style = styleFor(level)
    val fontSize = if (size == PillSize.Sm) 11.sp else 12.5.sp
    val horiz = if (size == PillSize.Sm) 8.dp else 10.dp
    val vert = if (size == PillSize.Sm) 3.dp else 4.dp
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(style.cardBg)
            .padding(horizontal = horiz, vertical = vert),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(style.iconRes),
            contentDescription = null,
            tint = style.tint,
            modifier = Modifier.size(11.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            time,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = fontSize),
            color = style.tint,
        )
    }
}
