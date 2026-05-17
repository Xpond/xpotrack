package com.xpotrack.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R
import com.xpotrack.app.data.model.Category
import com.xpotrack.app.ui.categories.parseHexColor
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun NotesCategoryContent(
    notes: List<NoteRow>,
    categories: List<Category>,
    onOpenNote: (Int) -> Unit,
    onManageCategories: () -> Unit,
) {
    QuickEntryStrip()
    val pinned = notes.filter { it.isPinned }
    if (pinned.isNotEmpty()) PinnedStrip(pinned, onOpenNote)
    Spacer(Modifier.height(20.dp))
    val groups = categories
        .map { cat -> cat to notes.filter { it.categoryId == cat.id && !it.isPinned } }
        .filter { (_, n) -> n.isNotEmpty() }
    groups.forEach { (cat, rows) ->
        CategoryGroup(cat, rows, onOpenNote = onOpenNote, onManageCategories = onManageCategories)
    }
    val uncategorized = notes.filter { it.categoryId == 0L && !it.isPinned }
    if (uncategorized.isNotEmpty()) {
        UncategorizedGroup(uncategorized, onOpenNote = onOpenNote, onManageCategories = onManageCategories)
    }
    NewCategoryButton(onClick = onManageCategories)
}

@Composable
private fun PinnedStrip(pinned: List<NoteRow>, onOpenNote: (Int) -> Unit) {
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
            pinned.forEach { PinnedCard(it, onOpenNote) }
            Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
private fun PinnedCard(note: NoteRow, onOpenNote: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(XpTokens.Surface1)
            .border(0.5.dp, XpTokens.Hair, RoundedCornerShape(14.dp))
            .clickable { onOpenNote(note.id) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(note.categoryName.uppercase(), style = MaterialTheme.typography.labelSmall, color = XpTokens.TealDim)
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CategoryGroup(cat: Category, notes: List<NoteRow>, onOpenNote: (Int) -> Unit, onManageCategories: () -> Unit) {
    val visible = notes.take(3)
    val moreCount = notes.size - visible.size
    Column(Modifier.padding(start = 22.dp, end = 22.dp, bottom = 22.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.pointerInput(cat.id) {
                detectTapGestures(onLongPress = { onManageCategories() })
            },
        ) {
            Box(
                Modifier.size(8.dp).clip(CircleShape).background(parseHexColor(cat.colorHex)),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                cat.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                color = XpTokens.Ink,
            )
            Spacer(Modifier.size(10.dp))
            CountChip(notes.size)
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
            CategoryNoteRow(note, isLast = i == visible.size - 1 && moreCount == 0, onOpenNote = onOpenNote)
        }
        if (moreCount > 0) {
            Spacer(Modifier.height(10.dp))
            Text("+ $moreCount more", style = MaterialTheme.typography.labelMedium, color = XpTokens.Ink3)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun UncategorizedGroup(notes: List<NoteRow>, onOpenNote: (Int) -> Unit, onManageCategories: () -> Unit) {
    val visible = notes.take(3)
    val moreCount = notes.size - visible.size
    Column(Modifier.padding(start = 22.dp, end = 22.dp, bottom = 22.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onLongPress = { onManageCategories() })
            },
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(XpTokens.Ink3))
            Spacer(Modifier.size(8.dp))
            Text(
                "Uncategorized",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                color = XpTokens.Ink2,
            )
            Spacer(Modifier.size(10.dp))
            CountChip(notes.size)
        }
        Spacer(Modifier.height(10.dp))
        visible.forEachIndexed { i, note ->
            CategoryNoteRow(note, isLast = i == visible.size - 1 && moreCount == 0, onOpenNote = onOpenNote)
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
private fun CategoryNoteRow(note: NoteRow, isLast: Boolean, onOpenNote: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { onOpenNote(note.id) }
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
private fun NewCategoryButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(0.5.dp, XpTokens.Hair2, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
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
