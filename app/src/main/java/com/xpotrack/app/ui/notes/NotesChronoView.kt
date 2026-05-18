package com.xpotrack.app.ui.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.ui.theme.XpTokens

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChronoNoteRow(
    note: NoteRow,
    showTag: Boolean,
    isLast: Boolean,
    onOpenNote: (Int) -> Unit,
    onLongPress: (NoteRow) -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onOpenNote(note.id) },
                onLongClick = { onLongPress(note) },
            )
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            note.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
            color = XpTokens.Ink,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (showTag) {
            Spacer(Modifier.width(12.dp))
            Text(
                note.categoryName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = XpTokens.Ink3,
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            note.when_,
            style = MaterialTheme.typography.labelSmall,
            color = XpTokens.Ink3,
        )
    }
    if (!isLast) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .height(0.5.dp)
                .background(XpTokens.Hair)
        )
    }
}
