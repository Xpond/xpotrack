package com.xpotrack.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun NotesCategoryContent(notes: List<NoteRow>) {
    QuickEntryStrip()
    val pinned = notes.filter { it.id in PinnedIds }
    if (pinned.isNotEmpty()) PinnedStrip(pinned)
    Spacer(Modifier.height(20.dp))
    val groups = Categories
        .map { cat -> cat to notes.filter { it.category == cat.name && it.id !in PinnedIds }.sortedByDescending { it.recency } }
        .filter { (_, n) -> n.isNotEmpty() }
    groups.forEach { (cat, rows) -> CategoryGroup(cat, rows) }
    NewCategoryButton()
}

@Composable
private fun PinnedStrip(pinned: List<NoteRow>) {
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_star),
                contentDescription = null,
                tint = XpTokens.TealDim,
                modifier = Modifier.size(11.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text("PINNED", style = MaterialTheme.typography.labelSmall, color = XpTokens.TealDim)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spacer(Modifier.width(6.dp))
            pinned.forEach { PinnedCard(it) }
            Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
private fun PinnedCard(note: NoteRow) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(XpTokens.Surface1)
            .border(0.5.dp, XpTokens.Hair, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(note.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = XpTokens.TealDim)
        Spacer(Modifier.height(6.dp))
        Text(
            note.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
            color = XpTokens.Ink,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            note.preview,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
            color = XpTokens.Ink2,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CategoryGroup(cat: Category, notes: List<NoteRow>) {
    val visible = notes.take(3)
    val moreCount = notes.size - visible.size
    Column(Modifier.padding(start = 22.dp, end = 22.dp, bottom = 22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                cat.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                color = XpTokens.Ink,
            )
            Spacer(Modifier.size(10.dp))
            CountChip(notes.size)
            if (cat.isCustom) {
                Spacer(Modifier.size(8.dp))
                Text("· custom".uppercase(), style = MaterialTheme.typography.labelMedium, color = XpTokens.TealDim.copy(alpha = 0.7f))
            }
            Spacer(Modifier.weight(1f))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = XpTokens.Ink3,
                modifier = Modifier.size(13.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        visible.forEachIndexed { i, note ->
            CategoryNoteRow(note, isLast = i == visible.size - 1 && moreCount == 0)
        }
        if (moreCount > 0) {
            Spacer(Modifier.height(10.dp))
            Text("+ $moreCount more", style = MaterialTheme.typography.labelMedium, color = XpTokens.Ink3)
        }
    }
}

@Composable
private fun CountChip(count: Int) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(XpTokens.Surface1)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified),
            color = XpTokens.Ink3,
        )
    }
}

@Composable
private fun CategoryNoteRow(note: NoteRow, isLast: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                note.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                color = XpTokens.Ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                note.when_,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified),
                color = XpTokens.Ink3,
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            note.preview,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
            color = XpTokens.Ink2,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
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

@Composable
private fun NewCategoryButton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(0.5.dp, XpTokens.Hair2, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = null,
                tint = XpTokens.Ink3,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("New category", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = XpTokens.Ink3)
        }
    }
}
