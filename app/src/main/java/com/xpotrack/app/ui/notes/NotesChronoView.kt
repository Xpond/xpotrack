package com.xpotrack.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun NotesChronoContent(notes: List<NoteRow>, onOpenNote: (Int) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        QuickEntryStrip()
        notes.forEachIndexed { i, note -> ChronoNoteRow(note, isLast = i == notes.size - 1, onOpenNote) }
    }
}

@Composable
private fun ChronoNoteRow(note: NoteRow, isLast: Boolean, onOpenNote: (Int) -> Unit) {
    Column(
        Modifier
            .clickable { onOpenNote(note.id) }
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                note.categoryName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = XpTokens.TealDim,
            )
            Spacer(Modifier.weight(1f))
            Text(
                note.when_,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                color = XpTokens.Ink3,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            note.title,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            color = XpTokens.Ink,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            note.preview,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, lineHeight = 21.sp),
            color = XpTokens.Ink2,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "${note.words} words",
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
            color = XpTokens.Ink3,
        )
    }
    if (!isLast) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(XpTokens.Hair)
        )
    }
}
