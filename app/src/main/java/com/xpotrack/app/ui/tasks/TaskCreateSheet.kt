package com.xpotrack.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xpotrack.app.data.model.ReminderLevel
import com.xpotrack.app.ui.components.styleFor
import com.xpotrack.app.ui.theme.XpTokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCreateSheet(
    vm: TaskCreateViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

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
            RepeatRow()

            Spacer(Modifier.height(14.dp))
            ScheduleButton(
                state = state,
                onClick = {
                    scope.launch {
                        if (vm.save() != null) onDismiss()
                    }
                },
            )
        }
    }
}

@Composable
private fun SheetGrabber() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 38.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(XpTokens.Ink3.copy(alpha = 0.35f))
        )
    }
}

@Composable
private fun TitleField(value: String, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth()) {
            if (value.isEmpty()) {
                Text(
                    "Task name",
                    style = TextStyle(fontSize = 18.sp, color = XpTokens.Ink3, fontWeight = FontWeight.Medium),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 18.sp, color = XpTokens.Ink, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(XpTokens.Teal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(XpTokens.Hair)
        )
    }
}

@Composable
private fun NotesField(value: String, onChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .clickable(interactionSource = interaction, indication = null) { focus.requestFocus() }
            .padding(vertical = 8.dp),
    ) {
        BasicTextField(
            value = value, onValueChange = onChange,
            textStyle = TextStyle(fontSize = 14.sp, color = XpTokens.Ink2, lineHeight = 20.sp),
            cursorBrush = SolidColor(XpTokens.Teal),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text("Notes (optional)", style = TextStyle(fontSize = 14.sp, color = XpTokens.Ink3))
                }
                inner()
            },
        )
    }
}

@Composable
private fun ReminderChips(active: ReminderLevel, onSelect: (ReminderLevel) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ReminderLevel.values().forEach { level ->
            val style = styleFor(level)
            val isActive = level == active
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isActive) style.cardBg else Color.Transparent)
                    .border(
                        width = 0.5.dp,
                        color = if (isActive) style.accent else XpTokens.Hair,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable { onSelect(level) }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(style.iconRes),
                    contentDescription = level.name,
                    tint = if (isActive) style.tint else XpTokens.Ink2,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    when (level) {
                        ReminderLevel.Silent -> "Silent"
                        ReminderLevel.Notify -> "Notify"
                        ReminderLevel.Alarm -> "Alarm"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                    color = if (isActive) style.tint else XpTokens.Ink2,
                )
            }
        }
    }
}

@Composable
private fun RepeatRow() {
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(XpTokens.Hair)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Repeat", color = XpTokens.Ink2, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp))
            Spacer(Modifier.weight(1f))
            Text(
                "Never  ›",
                color = XpTokens.Ink3,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
            )
        }
    }
}

@Composable
private fun ScheduleButton(state: TaskEditState, onClick: () -> Unit) {
    val enabled = state.title.isNotBlank()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) XpTokens.Teal else XpTokens.Teal.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Schedule for ${displayTime(state.hour, state.minute)} today",
            color = XpTokens.OnTeal,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

private fun displayTime(h: Int, m: Int): String {
    val isPm = h >= 12
    val hh = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    return "%d:%02d %s".format(hh, m, if (isPm) "PM" else "AM")
}
