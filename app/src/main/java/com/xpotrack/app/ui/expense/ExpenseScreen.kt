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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xpotrack.app.R
import com.xpotrack.app.data.model.expenseTotalPaise
import com.xpotrack.app.ui.components.DateTimeStrip
import com.xpotrack.app.ui.components.EmptyState
import com.xpotrack.app.ui.components.PinnedHeader
import com.xpotrack.app.ui.notes.CaretScrollEffect
import com.xpotrack.app.ui.notes.rememberCaretScroll
import com.xpotrack.app.ui.notes.TopHalo
import com.xpotrack.app.ui.tasks.MonthPickerDialog
import com.xpotrack.app.ui.theme.XpTokens
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

    val scroll = rememberScrollState()
    val caret = rememberCaretScroll(scroll)
    var focused by remember { mutableStateOf(false) }
    var viewportTop by remember { mutableFloatStateOf(0f) }
    if (focused && loaded) CaretScrollEffect(caret, draft.selection.start)

    val density = LocalDensity.current
    var headerPx by remember { mutableIntStateOf(0) }
    val headerDp = with(density) { headerPx.toDp() }

    Box(Modifier.fillMaxSize().background(XpTokens.Bg)) {
        TopHalo()
        Column(
            Modifier.fillMaxSize().imePadding()
                .onGloballyPositioned {
                    caret.viewportHeightPx = it.size.height
                    viewportTop = it.positionInRoot().y
                }
                .verticalScroll(scroll),
        ) {
            Spacer(Modifier.height(headerDp))
            if (listed.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(top = 160.dp), contentAlignment = Alignment.Center) {
                    EmptyState("Nothing logged", "Tap the month to pick a day")
                }
            }
            listed.forEachIndexed { i, day ->
                val open = day == openDay
                ExpenseDayBlock(
                    day = day,
                    isToday = day == today,
                    open = open,
                    text = textOf(day),
                    first = i == 0,
                    onToggle = { if (!open) focusDay = day; vm.toggle(day) },
                ) {
                    ExpenseDayField(
                        value = draft,
                        caret = caret,
                        modifier = Modifier
                            .onFocusChanged { focused = it.isFocused }
                            .onGloballyPositioned {
                                caret.fieldTopInScrollPx = (it.positionInRoot().y - viewportTop + scroll.value).toInt()
                            },
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
        PinnedHeader(onSize = { headerPx = it.height; caret.topInsetPx = it.height }) {
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
                    withStyle(SpanStyle(color = XpTokens.Teal)) { append(expenseAmount(totalPaise)) }
                },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 26.sp),
            )
            Spacer(Modifier.height(4.dp))
            Text("SPENT", style = MaterialTheme.typography.labelMedium, color = XpTokens.Ink3)
        }
    }
}

private fun monthTitle(m: YearMonth): String {
    val name = m.month.getDisplayName(JTextStyle.FULL, Locale.getDefault())
    return if (m.year == YearMonth.now(ZoneId.systemDefault()).year) name else "$name ${m.year}"
}
