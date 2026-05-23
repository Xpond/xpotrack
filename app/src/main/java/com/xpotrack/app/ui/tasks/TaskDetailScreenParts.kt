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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R
import com.xpotrack.app.data.model.Task
import com.xpotrack.app.ui.components.PillSize
import com.xpotrack.app.ui.components.ReminderStyle
import com.xpotrack.app.ui.components.XpReminderPill
import com.xpotrack.app.ui.notes.NoteRow
import com.xpotrack.app.ui.theme.GeistMono
import com.xpotrack.app.ui.theme.XpTokens

@Composable
internal fun TopBar(counter: String) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(counter, style = MaterialTheme.typography.labelMedium, color = XpTokens.Ink3)
    }
}

@Composable
internal fun HeroTime(task: Task, style: ReminderStyle) {
    val (h24, m) = parseHHmm(task.time)
    val isPm = h24 >= 12
    val h12 = ((h24 + 11) % 12) + 1
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "%d:%02d".format(h12, m),
            fontFamily = GeistMono, fontWeight = FontWeight.Medium,
            fontSize = 58.sp, lineHeight = 58.sp, letterSpacing = (-0.025).em,
            color = style.accent,
        )
        Text(
            if (isPm) "PM" else "AM",
            fontFamily = GeistMono, fontWeight = FontWeight.Medium,
            fontSize = 20.sp, color = XpTokens.Ink3,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Spacer(Modifier.weight(1f))
        Box(Modifier.padding(bottom = 8.dp)) {
            XpReminderPill(task.level, levelLabel(task.level), PillSize.Md)
        }
    }
}

@Composable
internal fun FieldsCard(task: Task, style: ReminderStyle, onAnyRow: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(XpTokens.Surface1)
            .border(0.5.dp, XpTokens.Hair, RoundedCornerShape(14.dp)),
    ) {
        FieldRow(R.drawable.ic_clock, "WHEN", "${dayLabel(task.dateEpochDay)} · ${formatTime12(task.time)}", onClick = onAnyRow)
        Divider()
        FieldRow(
            R.drawable.ic_reminder_alarm, "REMINDER",
            reminderSummary(task.level), valueColor = style.accent, onClick = onAnyRow,
        )
        Divider()
        FieldRow(R.drawable.ic_repeat, "REPEAT", repeatLabel(task.repeat, task.dateEpochDay), onClick = onAnyRow)
    }
}

@Composable
private fun FieldRow(iconRes: Int, label: String, value: String, valueColor: Color = XpTokens.Ink, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier.fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                .background(Color(0x0A5EEAD4)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(iconRes), null, tint = XpTokens.TealDim, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            label, fontFamily = GeistMono, fontSize = 11.5.sp,
            letterSpacing = 0.05.em, color = XpTokens.Ink3, modifier = Modifier.weight(1f),
        )
        Text(value, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = valueColor)
        Spacer(Modifier.width(8.dp))
        Icon(
            painterResource(R.drawable.ic_chevron_right), null,
            tint = XpTokens.Ink3, modifier = Modifier.size(11.dp),
        )
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(XpTokens.Hair))
}

@Composable
internal fun NotesArea(value: String, enabled: Boolean, onChange: (String) -> Unit) {
    val scroll = rememberScrollState()
    // Cap at ~3 lines (lineHeight 21sp × 3 ≈ 63dp + breathing room). Long notes
    // scroll internally; the field never pushes the link card or actions down.
    if (!enabled) {
        if (value.isNotBlank()) {
            Box(Modifier.fillMaxWidth().heightIn(max = 72.dp).verticalScroll(scroll)) {
                Text(value, color = XpTokens.Ink2, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
        return
    }
    Box(Modifier.fillMaxWidth().heightIn(max = 72.dp).verticalScroll(scroll)) {
        BasicTextField(
            value = value, onValueChange = onChange,
            textStyle = TextStyle(fontSize = 14.sp, lineHeight = 21.sp, color = XpTokens.Ink2),
            cursorBrush = SolidColor(XpTokens.Teal),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text("Add notes…", color = XpTokens.Ink3, fontSize = 14.sp, lineHeight = 21.sp)
                }
                inner()
            },
        )
    }
}

@Composable
internal fun LinkedNoteSection(note: NoteRow?, onPick: () -> Unit, onOpen: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text("Linked note".uppercase(),
            fontFamily = GeistMono, fontSize = 11.5.sp,
            letterSpacing = 0.05.em, color = XpTokens.Ink3)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(XpTokens.Surface1)
                .border(0.5.dp, XpTokens.Hair, RoundedCornerShape(12.dp))
                .clickable(onClick = if (note != null) onOpen else onPick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                    .background(XpTokens.Teal.copy(alpha = 0.06f))
                    .border(0.5.dp, XpTokens.Hair2, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_note), null,
                    tint = XpTokens.TealDim, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (note == null) {
                    Text("Link a note", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = XpTokens.Ink)
                    Text("Tap to pick from your notes", fontSize = 11.5.sp, color = XpTokens.Ink3)
                } else {
                    Text(note.title.ifBlank { "Untitled" },
                        fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = XpTokens.Ink, maxLines = 1)
                    if (note.preview.isNotBlank()) {
                        Text(note.preview, fontSize = 11.5.sp, color = XpTokens.Ink3, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            if (note != null) {
                Text("Change", color = XpTokens.Ink3, fontSize = 12.sp,
                    modifier = Modifier.clickable(onClick = onPick).padding(end = 8.dp))
            }
            Icon(painterResource(R.drawable.ic_chevron_right), null,
                tint = XpTokens.Ink3, modifier = Modifier.size(11.dp))
        }
    }
}

@Composable
internal fun ActionRow(done: Boolean, onMarkDone: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(12.dp))
                .background(if (done) XpTokens.Surface2 else XpTokens.Teal)
                .clickable(enabled = !done, onClick = onMarkDone),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_check), null,
                tint = if (done) XpTokens.Ink3 else XpTokens.OnTeal,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (done) "Done" else "Mark done",
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = if (done) XpTokens.Ink3 else XpTokens.OnTeal,
            )
        }
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                .border(0.5.dp, XpTokens.Hair2, RoundedCornerShape(12.dp))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_trash), "Delete", tint = XpTokens.Ink2, modifier = Modifier.size(16.dp))
        }
    }
}

