package com.xpotrack.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.xpotrack.app.R
import com.xpotrack.app.data.model.Category
import com.xpotrack.app.ui.components.ConfirmDeleteDialog
import com.xpotrack.app.ui.components.DateTimeStrip
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun NotesListScreen(
    notes: List<NoteRow>,
    categories: List<Category>,
    quick: QuickSummary,
    onOpenNote: (Int) -> Unit,
    onOpenQuick: () -> Unit,
    onDeleteNote: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // null = "All notes"; 0L = Uncategorized; >0 = a category id.
    var filterId by remember { mutableStateOf<Long?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<NoteRow?>(null) }

    val activeCategory = filterId?.let { id ->
        if (id == 0L) null else categories.firstOrNull { it.id == id }
    }
    val filterLabel = when {
        filterId == null -> null
        filterId == 0L -> "Uncategorized"
        else -> activeCategory?.name ?: "All notes"
    }
    val byCategory = when (filterId) {
        null -> notes
        0L -> notes.filter { it.categoryId == 0L }
        else -> notes.filter { it.categoryId == filterId }
    }
    val q = query.trim()
    val filtered = if (searchOpen && q.isNotEmpty()) {
        byCategory.filter { it.title.contains(q, ignoreCase = true) }
    } else byCategory

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(XpTokens.Bg),
    ) {
        TopHalo()
        Column(Modifier.fillMaxSize()) {
            if (searchOpen) {
                NotesSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    onClose = { searchOpen = false; query = "" },
                )
            } else {
                NotesHeader(onSearch = { searchOpen = true })
                NotesFilterBar(
                    label = filterLabel,
                    totalCount = notes.size,
                    categories = categories,
                    notes = notes,
                    onPick = { filterId = it },
                    onClear = { filterId = null },
                )
            }
            QuickEntryStrip(count = quick.count, oldestLeft = quick.oldestLeft, onClick = onOpenQuick)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                filtered.forEachIndexed { i, note ->
                    ChronoNoteRow(
                        note = note,
                        showTag = filterId == null,
                        isLast = i == filtered.size - 1,
                        onOpenNote = onOpenNote,
                        onLongPress = { pendingDelete = it },
                    )
                }
                Spacer(Modifier.height(100.dp))
            }
        }
        NotesFab(Modifier.align(Alignment.BottomEnd), onClick = { onOpenNote(0) })
    }
    pendingDelete?.let { note ->
        ConfirmDeleteDialog(
            title = "Delete note?",
            subject = note.title.ifBlank { "Untitled" },
            onCancel = { pendingDelete = null },
            onConfirm = {
                onDeleteNote(note.id)
                pendingDelete = null
            },
        )
    }
}

@Composable
private fun TopHalo() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                Brush.radialGradient(
                    0f to XpTokens.TealGlow,
                    0.7f to Color.Transparent,
                )
            )
    )
}

@Composable
private fun NotesHeader(onSearch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 18.dp, top = 14.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            DateTimeStrip()
            Spacer(Modifier.height(14.dp))
            Text("Notes", style = MaterialTheme.typography.displayLarge, color = XpTokens.Ink)
        }
        IconBtn(R.drawable.ic_search, "Search", tint = XpTokens.Ink2, onClick = onSearch)
    }
}

@Composable
private fun IconBtn(
    iconRes: Int,
    contentDesc: String,
    tint: Color,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .border(0.5.dp, XpTokens.Hair2, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDesc,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun NotesFab(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .padding(end = 22.dp, bottom = 86.dp)
            .size(56.dp)
            .shadow(elevation = 18.dp, shape = CircleShape, ambientColor = XpTokens.Teal, spotColor = XpTokens.Teal)
            .clip(CircleShape)
            .background(XpTokens.Teal)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = "New note",
            tint = XpTokens.OnTeal,
            modifier = Modifier.size(22.dp),
        )
    }
}
