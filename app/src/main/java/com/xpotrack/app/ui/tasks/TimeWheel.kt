package com.xpotrack.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun TimeWheel(hour: Int, minute: Int, onHour: (Int) -> Unit, onMinute: (Int) -> Unit) {
    val isPm = hour >= 12
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val hourValues = remember { (1..12).toList() }
    val minuteValues = remember { (0..59).toList() }
    val ampmValues = remember { listOf("AM", "PM") }

    Box(
        Modifier
            .fillMaxWidth()
            .height(WheelHeight + 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0A5EEAD4))
            .border(0.5.dp, XpTokens.Hair, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        // center highlight band — 0.5dp hairlines at top + bottom of selected row
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .height(WheelItemHeight)
                .fillMaxWidth()
                .border(0.5.dp, XpTokens.Hair2, RoundedCornerShape(0.dp))
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            WheelPicker(
                values = hourValues,
                selectedIndex = hourValues.indexOf(displayHour).coerceAtLeast(0),
                onIndexChange = { idx -> onHour(to24h(hourValues[idx], isPm)) },
                width = 56.dp,
                format = { it.toString() },
            )
            Text(
                ":",
                color = XpTokens.Ink3,
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = WheelMonoFamily,
                ),
                modifier = Modifier.padding(horizontal = 2.dp),
            )
            WheelPicker(
                values = minuteValues,
                selectedIndex = minute.coerceIn(0, 59),
                onIndexChange = { idx -> onMinute(minuteValues[idx]) },
                width = 56.dp,
                format = { "%02d".format(it) },
            )
            Spacer(Modifier.width(8.dp))
            WheelPicker(
                values = ampmValues,
                selectedIndex = if (isPm) 1 else 0,
                onIndexChange = { idx -> onHour(to24h(displayHour, idx == 1)) },
                width = 48.dp,
                bigFontSize = 18.sp,
                smallFontSize = 14.sp,
                format = { it },
            )
        }
    }
}

private fun to24h(twelveHour: Int, isPm: Boolean): Int = when {
    isPm && twelveHour == 12 -> 12
    isPm -> twelveHour + 12
    !isPm && twelveHour == 12 -> 0
    else -> twelveHour
}
