package com.xpotrack.app.ui.expense

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R
import com.xpotrack.app.ui.components.DateTimeStrip
import com.xpotrack.app.ui.theme.XpTokens
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

@Composable
internal fun ExpenseHeader(month: YearMonth, totalPaise: Long, onPickMonth: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 18.dp, bottom = 18.dp)) {
        DateTimeStrip()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Row(
                Modifier.weight(1f).padding(end = 12.dp).clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPickMonth,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    monthTitle(month), style = MaterialTheme.typography.displayLarge, color = XpTokens.Ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                Icon(painterResource(R.drawable.ic_caret_down), "Pick month", tint = XpTokens.Ink3, modifier = Modifier.size(14.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = XpTokens.Ink3)) { append("₹") }
                        withStyle(SpanStyle(color = XpTokens.Teal)) { append(expenseAmount(totalPaise)) }
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 26.sp),
                )
                Spacer(Modifier.height(4.dp))
                Text("SPENT", style = MaterialTheme.typography.labelMedium, color = XpTokens.Ink3)
            }
        }
    }
}

private fun monthTitle(m: YearMonth): String {
    val name = m.month.getDisplayName(JTextStyle.FULL, Locale.getDefault())
    return if (m.year == YearMonth.now(ZoneId.systemDefault()).year) name else "$name ${m.year}"
}
