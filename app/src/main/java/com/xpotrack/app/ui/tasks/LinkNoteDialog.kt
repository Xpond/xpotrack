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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R
import com.xpotrack.app.ui.notes.NoteRow
import com.xpotrack.app.ui.theme.XpTokens

// Note picker. Search-driven and bounded to ~200 results per query so it
// stays usable at million-note scale. Caller owns the query state so the
// VM can run the search through SQLite (LIKE on title) instead of filtering
// a fully-materialized list in memory.
@Composable
fun LinkNoteDialog(
    results: List<NoteRow>,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedId: Long?,
    onPick: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    DialogCard(onDismiss) {
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
            SearchBox(value = query, onChange = onQueryChange)
            Spacer(Modifier.height(10.dp))
            if (results.isEmpty()) {
                EmptyState(query)
            } else {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(results, key = { it.id }) { row ->
                        NoteOption(row, isSelected = row.id.toLong() == selectedId,
                            onClick = { onPick(row.id.toLong()) })
                        Spacer(Modifier.height(4.dp))
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
    SelectableRow(isSelected, onClick, horizontalPadding = 12, verticalPadding = 10) {
        Box(
            Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                .background(XpTokens.Teal.copy(alpha = 0.06f))
                .border(0.5.dp, XpTokens.Hair2, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_note), null, tint = XpTokens.TealDim, modifier = Modifier.size(13.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(row.title.ifBlank { "Untitled" }, fontSize = 13.5.sp, fontWeight = FontWeight.Medium,
                color = if (isSelected) XpTokens.Teal else XpTokens.Ink, maxLines = 1)
            if (row.preview.isNotBlank()) Text(row.preview, fontSize = 11.5.sp, color = XpTokens.Ink3, maxLines = 1)
        }
    }
}

@Composable
private fun EmptyState(query: String) {
    Box(
        Modifier.fillMaxWidth().padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (query.isBlank()) "No notes yet" else "No notes match \"$query\"",
            color = XpTokens.Ink3, fontSize = 13.sp,
        )
    }
}
