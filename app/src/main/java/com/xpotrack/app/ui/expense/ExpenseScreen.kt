package com.xpotrack.app.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xpotrack.app.R
import com.xpotrack.app.data.model.expenseTotalPaise
import com.xpotrack.app.data.model.parseExpenseLine
import com.xpotrack.app.ui.components.DateTimeStrip
import com.xpotrack.app.ui.components.EmptyState
import com.xpotrack.app.ui.components.PinnedHeader
import com.xpotrack.app.ui.notes.TopHalo
import com.xpotrack.app.ui.tasks.MonthPickerDialog
import com.xpotrack.app.ui.theme.XpTokens
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

@Composable
fun ExpenseScreen(vm: ExpenseViewModel) {
    val month by vm.month.collectAsStateWithLifecycle()
    val openDay by vm.openDay.collectAsStateWithLifecycle()
    val days by vm.days.collectAsStateWithLifecycle()
    val loggedDates by vm.loggedDates.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now(ZoneId.systemDefault()).toEpochDay() }
    var pickerOpen by remember { mutableStateOf(false) }

    // The open day's text lives here, loaded once per open and saved on every
    // edit (same model as LockedNoteScreen). readOnly until the load lands so
    // early keystrokes can't overwrite the stored day with a blank draft.
    var draft by remember(openDay) { mutableStateOf(TextFieldValue("")) }
    var loaded by remember(openDay) { mutableStateOf(false) }
    // Set when the user opens a day by tapping it — the default-open today
    // on tab entry shouldn't pop the keyboard.
    var focusDay by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(openDay) {
        val day = openDay ?: return@LaunchedEffect
        val text = vm.load(day)
        draft = TextFieldValue(text, TextRange(text.length))
        loaded = true
    }

    val inMonth = { d: Long -> YearMonth.from(LocalDate.ofEpochDay(d)) == month }
    val listed = remember(days, openDay, month) {
        (days.keys + listOfNotNull(openDay, today).filter(inMonth)).sortedDescending()
    }
    val textOf = { d: Long -> if (d == openDay && loaded) draft.text else days[d].orEmpty() }
    val monthTotal = listed.sumOf { expenseTotalPaise(textOf(it)) }

    val density = LocalDensity.current
    var headerPx by remember { mutableIntStateOf(0) }
    val headerDp = with(density) { headerPx.toDp() }

    Box(Modifier.fillMaxSize().background(XpTokens.Bg)) {
        TopHalo()
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(headerDp))
            if (listed.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(top = 160.dp), contentAlignment = Alignment.Center) {
                    EmptyState("Nothing logged", "Tap the month to pick a day")
                }
            }
            listed.forEachIndexed { i, day ->
                val open = day == openDay
                DayBlock(
                    day = day,
                    isToday = day == today,
                    open = open,
                    text = textOf(day),
                    first = i == 0,
                    onToggle = { if (!open) focusDay = day; vm.toggle(day) },
                ) {
                    ExpenseDayField(
                        value = draft,
                        onValueChange = { v ->
                            if (v.text != draft.text) vm.save(day, v.text)
                            draft = v
                        },
                        readOnly = !loaded,
                        focusNow = loaded && focusDay == day,
                    )
                }
            }
            Spacer(Modifier.height(120.dp))
        }
        PinnedHeader(onSize = { headerPx = it.height }) {
            Header(month = month, totalPaise = monthTotal, onPickMonth = { pickerOpen = true })
        }
    }
    if (pickerOpen) {
        MonthPickerDialog(
            selectedEpochDay = openDay ?: month.atDay(1).toEpochDay(),
            datesWithTasks = loggedDates,
            onPick = { d -> focusDay = d; vm.pick(d); pickerOpen = false },
            onDismiss = { pickerOpen = false },
        )
    }
}

@Composable
private fun Header(month: YearMonth, totalPaise: Long, onPickMonth: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 22.dp, end = 18.dp, bottom = 18.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            DateTimeStrip()
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPickMonth,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(monthTitle(month), style = MaterialTheme.typography.displayLarge, color = XpTokens.Ink, maxLines = 1)
                Spacer(Modifier.width(8.dp))
                Icon(painterResource(R.drawable.ic_caret_down), "Pick month", tint = XpTokens.Ink3, modifier = Modifier.size(14.dp))
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = XpTokens.Ink3)) { append("₹") }
                    withStyle(SpanStyle(color = XpTokens.Teal)) { append(amount(totalPaise)) }
                },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 26.sp),
            )
            Spacer(Modifier.height(4.dp))
            Text("SPENT", style = MaterialTheme.typography.labelMedium, color = XpTokens.Ink3)
        }
    }
}

@Composable
private fun DayBlock(
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
                "₹" + amount(expenseTotalPaise(text)), fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = if (open) XpTokens.Teal else XpTokens.Ink,
            )
        }
        if (open) Box(Modifier.padding(start = 50.dp, bottom = 10.dp)) { field() }
    }
}

private fun preview(text: String): String =
    text.split('\n').mapNotNull { line -> parseExpenseLine(line)?.label?.takeIf { it.isNotEmpty() } }
        .joinToString(", ")

private fun monthTitle(m: YearMonth): String {
    val name = m.month.getDisplayName(JTextStyle.FULL, Locale.getDefault())
    return if (m.year == YearMonth.now(ZoneId.systemDefault()).year) name else "$name ${m.year}"
}

// Indian digit grouping (1,23,456). Paise only shown when non-zero.
private val rupees = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 2 }

private fun amount(paise: Long): String = rupees.format(BigDecimal.valueOf(paise, 2))
