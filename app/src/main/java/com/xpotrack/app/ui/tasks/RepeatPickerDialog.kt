package com.xpotrack.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R
import com.xpotrack.app.ui.theme.XpTokens

// Four-option repeat picker. Surface matches MonthPickerDialog (rounded card,
// surface1 background, hair border) so both pickers feel like one family.
@Composable
fun RepeatPickerDialog(
    selected: String,
    epochDay: Long,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf("none", "daily", "weekdays", "weekly")
    DialogCard(onDismiss) {
        Column {
            Text(
                "Repeat".uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = XpTokens.Ink3,
            )
            Spacer(Modifier.height(12.dp))
            options.forEachIndexed { i, rule ->
                RepeatOption(
                    label = repeatLabel(rule, epochDay),
                    isSelected = rule == selected,
                    onClick = { onPick(rule) },
                )
                if (i < options.lastIndex) Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
internal fun SelectableRow(
    isSelected: Boolean,
    onClick: () -> Unit,
    horizontalPadding: Int = 14,
    verticalPadding: Int = 14,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) XpTokens.Teal.copy(alpha = 0.08f) else XpTokens.Surface2)
            .border(0.5.dp, if (isSelected) XpTokens.Teal else XpTokens.Hair, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding.dp, vertical = verticalPadding.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
        if (isSelected) Icon(painterResource(R.drawable.ic_check), null, tint = XpTokens.Teal, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun RepeatOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    SelectableRow(isSelected, onClick) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = if (isSelected) XpTokens.Teal else XpTokens.Ink,
            modifier = Modifier.weight(1f),
        )
    }
}
