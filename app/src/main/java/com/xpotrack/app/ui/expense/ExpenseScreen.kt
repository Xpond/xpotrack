package com.xpotrack.app.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xpotrack.app.data.model.expenseTotalPaise
import com.xpotrack.app.ui.components.EmptyState
import com.xpotrack.app.ui.components.PinnedHeader
import com.xpotrack.app.ui.notes.CaretScrollEffect
import com.xpotrack.app.ui.notes.rememberCaretScroll
import com.xpotrack.app.ui.notes.TopHalo
import com.xpotrack.app.ui.tasks.MonthPickerDialog
import com.xpotrack.app.ui.theme.XpTokens
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import kotlinx.coroutines.delay

@Composable
fun ExpenseScreen(vm: ExpenseViewModel) {
    val month by vm.month.collectAsStateWithLifecycle()
    val openDay by vm.openDay.collectAsStateWithLifecycle()
    val days by vm.days.collectAsStateWithLifecycle()
    val loggedDates by vm.loggedDates.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(vm, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.onEnter()
            while (true) {
                delay(millisUntilNextDay(ZonedDateTime.now()))
                vm.refreshToday()
            }
        }
    }
    var pickerOpen by remember { mutableStateOf(false) }

    // The open day's text lives here, loaded once per open and saved on every
    // edit. readOnly until the load lands so early keystrokes can't overwrite
    // the stored day with a blank draft.
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

    val listed = expenseDates(days.keys, openDay, today, month)
    val textOf = { d: Long -> if (d == openDay && loaded) draft.text else days[d].orEmpty() }
    val monthTotal = listed.sumOf { expenseTotalPaise(textOf(it)) }

    val scroll = rememberScrollState()
    val caret = rememberCaretScroll(scroll)
    var focused by remember { mutableStateOf(false) }
    var viewportTop by remember { mutableFloatStateOf(0f) }
    if (focused && loaded) {
        CaretScrollEffect(caret, draft.selection.start)
        // Day rows above the field shift it as they load, unlike the other editors.
        LaunchedEffect(caret.fieldTopInScrollPx) { caret.bringCaretIntoView(draft.selection.start) }
    }

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
                key(day) {
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
            }
            Spacer(Modifier.height(120.dp))
        }
        PinnedHeader(onSize = { headerPx = it.height; caret.topInsetPx = it.height }) {
            ExpenseHeader(month = month, totalPaise = monthTotal, onPickMonth = { pickerOpen = true })
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

internal fun expenseDates(days: Set<Long>, openDay: Long?, today: Long, month: YearMonth): List<Long> =
    (days + listOfNotNull(openDay, today))
        .filter { YearMonth.from(LocalDate.ofEpochDay(it)) == month }
        .sortedDescending()

internal fun millisUntilNextDay(now: ZonedDateTime): Long =
    Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(now.zone))
        .toMillis().coerceAtLeast(1)
