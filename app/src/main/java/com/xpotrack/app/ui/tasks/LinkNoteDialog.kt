package com.xpotrack.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xpotrack.app.R
import com.xpotrack.app.ui.notes.NoteRow
import com.xpotrack.app.ui.theme.XpTokens

// Note picker. Lists every non-locked note; tap to link, tap the current
// selection to unlink. Matches RepeatPickerDialog's card surface.
@Composable
fun LinkNoteDialog(
    notes: List<NoteRow>,
    selectedId: Long?,
    onPick: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(notes, query) {
        if (query.isBlank()) notes
        else notes.filter { it.title.contains(query, ignoreCase = true) ||
            it.preview.contains(query, ignoreCase = true) }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier.padding(horizontal = 24.dp).fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(XpTokens.Surface1)
                .border(0.5.dp, XpTokens.Hair, RoundedCornerShape(20.dp))
                .padding(18.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Link a note".uppercase(),
                        style = MaterialTheme.typography.labelMedium, color = XpTokens.Ink3)
                    Spacer(Modifier.weight(1f))
                    if (selectedId != null) {
                        Text("Unlink", color = XpTokens.Ink3, fontSize = 12.sp,
                            modifier = Modifier.clickable { onPick(null) })
                    }
                }
                Spacer(Modifier.height(12.dp))
                SearchBox(value = query, onChange = { query = it })
                Spacer(Modifier.height(10.dp))
                if (filtered.isEmpty()) {
                    EmptyState()
                } else {
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(filtered, key = { it.id }) { row ->
                            NoteOption(row, isSelected = row.id.toLong() == selectedId,
                                onClick = { onPick(row.id.toLong()) })
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBox(value: String, onChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(XpTokens.Surface2)
            .border(0.5.dp, XpTokens.Hair, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_search), null,
            tint = XpTokens.Ink3, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Search notes",
                style = TextStyle(fontSize = 13.sp, color = XpTokens.Ink3))
            BasicTextField(
                value = value, onValueChange = onChange, singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = XpTokens.Ink),
                cursorBrush = SolidColor(XpTokens.Teal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun NoteOption(row: NoteRow, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) XpTokens.Teal.copy(alpha = 0.08f) else XpTokens.Surface2)
            .border(
                width = 0.5.dp,
                color = if (isSelected) XpTokens.Teal else XpTokens.Hair,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                .background(XpTokens.Teal.copy(alpha = 0.06f))
                .border(0.5.dp, XpTokens.Hair2, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_note), null,
                tint = XpTokens.TealDim, modifier = Modifier.size(13.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(row.title.ifBlank { "Untitled" },
                fontSize = 13.5.sp, fontWeight = FontWeight.Medium,
                color = if (isSelected) XpTokens.Teal else XpTokens.Ink,
                maxLines = 1)
            if (row.preview.isNotBlank()) {
                Text(row.preview, fontSize = 11.5.sp, color = XpTokens.Ink3, maxLines = 1)
            }
        }
        if (isSelected) {
            Icon(painterResource(R.drawable.ic_check), null,
                tint = XpTokens.Teal, modifier = Modifier.size(13.dp))
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        Modifier.fillMaxWidth().padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("No notes found", color = XpTokens.Ink3, fontSize = 13.sp)
    }
}
