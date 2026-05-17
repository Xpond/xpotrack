package com.xpotrack.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.ui.theme.XpTokens

private data class DayChip(val label: String, val date: String, val active: Boolean = false, val faded: Boolean = false)

private val Days = listOf(
    DayChip("Thu", "15", faded = true),
    DayChip("Fri", "16", active = true),
    DayChip("Sat", "17"),
    DayChip("Sun", "18"),
    DayChip("Mon", "19"),
    DayChip("Tue", "20"),
    DayChip("Wed", "21"),
)

@Composable
fun DayChipStrip(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Days.forEach { day -> DayChipBox(day, Modifier.weight(1f)) }
    }
}

@Composable
private fun DayChipBox(day: DayChip, modifier: Modifier = Modifier) {
    val borderColor = if (day.active) XpTokens.Teal else Color.Transparent
    val bg = if (day.active) Color(0x1A5EEAD4) else Color.Transparent
    val labelColor = if (day.active) XpTokens.Teal else XpTokens.Ink3
    val dateColor = if (day.active) XpTokens.Teal else XpTokens.Ink2
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(0.5.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp)
            .alpha(if (day.faded) 0.4f else 1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            day.label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.5.sp),
            color = labelColor,
        )
        Text(
            day.date,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 16.sp),
            color = dateColor,
        )
    }
}
