package com.xpotrack.app.ui.tasks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xpotrack.app.ui.components.XpPrimaryButton
import com.xpotrack.app.ui.theme.XpTokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCreateSheet(
    vm: TaskCreateViewModel,
    datesWithTasks: Set<Long> = emptySet(),
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by vm.state.collectAsStateWithLifecycle()
    val allNotes by vm.allNotes.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var calendarOpen by remember { mutableStateOf(false) }
    var repeatOpen by remember { mutableStateOf(false) }
    var linkOpen by remember { mutableStateOf(false) }

    if (calendarOpen) {
        MonthPickerDialog(
            selectedEpochDay = state.dateEpochDay,
            datesWithTasks = datesWithTasks,
            disablePast = true,
            onPick = { vm.setDate(it); calendarOpen = false },
            onDismiss = { calendarOpen = false },
        )
    }

    if (repeatOpen) {
        RepeatPickerDialog(
            selected = state.repeat,
            epochDay = state.dateEpochDay,
            onPick = { vm.setRepeat(it); repeatOpen = false },
            onDismiss = { repeatOpen = false },
        )
    }

    if (linkOpen) {
        LinkNoteDialog(
            notes = allNotes,
            selectedId = state.linkedNoteId,
            onPick = { vm.setLinkedNote(it); linkOpen = false },
            onDismiss = { linkOpen = false },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = XpTokens.Surface1,
        contentColor = XpTokens.Ink,
        dragHandle = { SheetGrabber() },
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
        ) {
            Text(
                (if (state.isNew) "New task" else "Edit task").uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = XpTokens.Ink3,
            )
            Spacer(Modifier.height(12.dp))

            TitleField(
                value = state.title,
                onChange = vm::setTitle,
            )

            Spacer(Modifier.height(12.dp))
            NotesField(value = state.notes, onChange = vm::setNotes)

            Spacer(Modifier.height(18.dp))
            Text("Date".uppercase(), style = MaterialTheme.typography.labelMedium, color = XpTokens.Ink3)
            Spacer(Modifier.height(8.dp))
            DateRow(
                epochDay = state.dateEpochDay,
                onClick = { calendarOpen = true },
            )

            Spacer(Modifier.height(18.dp))
            Text("Time".uppercase(), style = MaterialTheme.typography.labelMedium, color = XpTokens.Ink3)
            Spacer(Modifier.height(10.dp))
            TimeWheel(
                hour = state.hour,
                minute = state.minute,
                onHour = vm::setHour,
                onMinute = vm::setMinute,
            )

            Spacer(Modifier.height(18.dp))
            Text("Reminder".uppercase(), style = MaterialTheme.typography.labelMedium, color = XpTokens.Ink3)
            Spacer(Modifier.height(10.dp))
            ReminderChips(active = state.level, onSelect = vm::setLevel)

            Spacer(Modifier.height(16.dp))
            RepeatRow(
                rule = state.repeat,
                epochDay = state.dateEpochDay,
                onClick = { repeatOpen = true },
            )

            LinkNoteRow(
                title = state.linkedNoteId?.let { id ->
                    allNotes.firstOrNull { it.id.toLong() == id }?.title?.ifBlank { "Untitled" }
                },
                onClick = { linkOpen = true },
            )

            Spacer(Modifier.height(14.dp))
            XpPrimaryButton(
                label = "Schedule for ${formatTime12(state.timeHHmm)} ${relativeDay(state.dateEpochDay)}",
                enabled = state.title.isNotBlank(),
                onClick = { scope.launch { if (vm.save() != null) onDismiss() } },
            )
        }
    }
}
