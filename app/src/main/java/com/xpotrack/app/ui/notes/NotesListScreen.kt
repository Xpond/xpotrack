package com.xpotrack.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.xpotrack.app.R
import com.xpotrack.app.data.model.Category
import com.xpotrack.app.ui.components.ConfirmDeleteDialog
import com.xpotrack.app.ui.components.DateTimeStrip
import com.xpotrack.app.ui.components.NoteActionSheet
import com.xpotrack.app.ui.components.PinnedHeader
import com.xpotrack.app.ui.components.XpFab
import com.xpotrack.app.ui.components.XpIconBtn
import com.xpotrack.app.ui.quick.QuickNoteEntry
import com.xpotrack.app.ui.quick.QuickRow
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun NotesListScreen(
    notes: List<FeedItem.Note>,
    quicks: List<FeedItem.Quick>,
    categories: List<Category>,
    onOpenNote: (Int) -> Unit,
    onComposeQuick: () -> Unit,
    onOpenQuickNote: (Long) -> Unit,
    onKeepQuick: (Long) -> Unit,
    onDeleteQuick: (Long) -> Unit,
    onDeleteNote: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // null = "All notes"; 0L = Uncategorized; >0 = a category id.
    var filterId by remember { mutableStateOf<Long?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var sheetNote by remember { mutableStateOf<NoteRow?>(null) }
    var pendingDeleteNote by remember { mutableStateOf<NoteRow?>(null) }
    var pendingDeleteQuick by remember { mutableStateOf<QuickRow?>(null) }
    val context = LocalContext.current

    val activeCategory = filterId?.let { id ->
        if (id == 0L) null else categories.firstOrNull { it.id == id }
    }
    val filterLabel = when {
        filterId == null -> null
        filterId == 0L -> "Uncategorized"
        else -> activeCategory?.name ?: "All notes"
    }
    val notesOnly = notes.map { it.row }
    // Merge here instead of in the VM so notes paint the instant they arrive,
    // even if quick notes haven't emitted yet. Quick notes belong to "All".
    val feed: List<FeedItem> = remember(notes, quicks) {
        (notes + quicks).sortedByDescending { it.sortKey }
    }
    val byCategory: List<FeedItem> = when (filterId) {
        null -> feed
        0L -> feed.filter { it is FeedItem.Note && it.row.categoryId == 0L }
        else -> feed.filter { it is FeedItem.Note && it.row.categoryId == filterId }
    }
    val q = query.trim()
    val filtered: List<FeedItem> = if (searchOpen && q.isNotEmpty()) {
        byCategory.filter { item ->
            when (item) {
                is FeedItem.Note -> item.row.title.contains(q, ignoreCase = true)
                is FeedItem.Quick -> item.row.text.contains(q, ignoreCase = true)
            }
        }
    } else byCategory

    val density = LocalDensity.current
    var headerPx by remember { mutableStateOf(0) }
    val headerDp = with(density) { headerPx.toDp() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(XpTokens.Bg),
    ) {
        TopHalo()
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(headerDp))
            filtered.forEachIndexed { i, item ->
                val isLast = i == filtered.size - 1
                when (item) {
                    is FeedItem.Note -> ChronoNoteRow(
                        note = item.row,
                        showTag = filterId == null,
                        isLast = isLast,
                        onOpenNote = onOpenNote,
                        onLongPress = { sheetNote = it },
                    )
                    is FeedItem.Quick -> QuickNoteEntry(
                        row = item.row,
                        isLast = isLast,
                        onKeep = { onKeepQuick(item.row.id) },
                        onOpen = { onOpenQuickNote(item.row.id) },
                        onLongPress = { pendingDeleteQuick = item.row },
                    )
                }
            }
            Spacer(Modifier.height(100.dp))
        }
        PinnedHeader(onSize = { headerPx = it.height }) {
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
                    totalCount = notesOnly.size,
                    categories = categories,
                    notes = notesOnly,
                    onPick = { filterId = it },
                    onClear = { filterId = null },
                )
            }
            QuickEntryStrip(onCompose = onComposeQuick)
        }
        XpFab(
            R.drawable.ic_plus, "New note", shadow = true,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 50.dp, bottom = 130.dp),
            onClick = { onOpenNote(0) },
        )
    }
    sheetNote?.let { note ->
        NoteActionSheet(
            subject = note.title.ifBlank { "Untitled" },
            onDismiss = { sheetNote = null },
            onShare = {
                shareNoteAsMarkdown(context, note.title, note.preview)
                sheetNote = null
            },
            onDelete = {
                pendingDeleteNote = note
                sheetNote = null
            },
        )
    }
    pendingDeleteNote?.let { note ->
        ConfirmDeleteDialog(
            title = "Delete note?",
            subject = note.title.ifBlank { "Untitled" },
            onCancel = { pendingDeleteNote = null },
            onConfirm = {
                onDeleteNote(note.id)
                pendingDeleteNote = null
            },
        )
    }
    pendingDeleteQuick?.let { row ->
        ConfirmDeleteDialog(
            title = "Delete quick note?",
            subject = row.text.lineSequence().firstOrNull()?.take(60).orEmpty().ifBlank { "Untitled" },
            onCancel = { pendingDeleteQuick = null },
            onConfirm = {
                onDeleteQuick(row.id)
                pendingDeleteQuick = null
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
        XpIconBtn(R.drawable.ic_search, "Search", tint = XpTokens.Ink2, border = true, onClick = onSearch)
    }
}

