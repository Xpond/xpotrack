package com.xpotrack.app.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.data.model.expenseTotalPaise
import com.xpotrack.app.data.model.parseExpenseLine
import com.xpotrack.app.ui.theme.XpTokens
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.YearMonth
import java.time.LocalDate
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

@Composable
internal fun ExpenseDayBlock(
    day: Long,
    isToday: Boolean,
    open: Boolean,
    text: String,
    first: Boolean,
    onToggle: () -> Unit,
    field: @Composable () -> Unit,
) {
    val date = remember(day) { LocalDate.ofEpochDay(day) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
        if (!first) Box(Modifier.fillMaxWidth().height(0.5.dp).background(XpTokens.Hair))
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.width(34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    date.dayOfMonth.toString(), fontSize = 18.sp, fontWeight = FontWeight.Medium,
                    color = if (isToday) XpTokens.Teal else XpTokens.Ink,
                )
                Text(
                    date.dayOfWeek.getDisplayName(JTextStyle.SHORT, Locale.getDefault()).uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.5.sp), color = XpTokens.Ink3,
                )
            }
            Spacer(Modifier.width(16.dp))
            if (open) {
                Text(
                    if (isToday) "TODAY" else "EDITING", style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                    color = XpTokens.Teal, modifier = Modifier.weight(1f),
                )
            } else {
                Text(
                    preview(text), fontSize = 12.5.sp, color = XpTokens.Ink2,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "₹" + expenseAmount(expenseTotalPaise(text)), fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = if (open) XpTokens.Teal else XpTokens.Ink,
            )
        }
        if (open) Box(Modifier.padding(start = 50.dp, bottom = 10.dp)) { field() }
    }
}

private fun preview(text: String): String =
    text.lineSequence().filter { it.isNotBlank() }.map { line ->
        parseExpenseLine(line)?.label?.takeIf { it.isNotEmpty() } ?: line.trim()
    }.joinToString(", ")

// Indian digit grouping (1,23,456). Paise only shown when non-zero.
private val rupees = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 2 }

internal fun expenseAmount(paise: Long): String = rupees.format(BigDecimal.valueOf(paise, 2))

internal fun expenseDates(days: Set<Long>, openDay: Long?, today: Long, month: YearMonth): List<Long> =
    (days + listOfNotNull(openDay, today))
        .filter { YearMonth.from(LocalDate.ofEpochDay(it)) == month }
        .sortedDescending()
