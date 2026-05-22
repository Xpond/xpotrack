package com.xpotrack.app.ui.notes

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.xpotrack.app.ui.components.EmptyState
import com.xpotrack.app.ui.components.PinnedHeader
import com.xpotrack.app.ui.components.SelectionBar
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
    onNewNote: (Long) -> Unit,
    onComposeQuick: () -> Unit,
    onOpenQuickNote: (Long) -> Unit,
    onKeepQuick: (Long) -> Unit,
    onDeleteQuick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onDeleteNote: (Int) -> Unit = {},
) {
    // null = "All notes"; 0L = Uncategorized; >0 = a category id.
    // Saveable so the filter survives navigating to the editor and back.
    var filterId by rememberSaveable { mutableStateOf<Long?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var selectMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var pendingBulkDelete by remember { mutableStateOf(false) }
    var pendingDeleteQuick by remember { mutableStateOf<QuickRow?>(null) }
    val context = LocalContext.current
    BackHandler(enabled = selectMode) {
        selectMode = false; selected = emptySet()
    }

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
    // Multi-select operates on the currently visible notes — not the full
    // collection. Select-All in a filtered view picks only what the user sees.
    val visibleNotes: List<NoteRow> = filtered.mapNotNull { (it as? FeedItem.Note)?.row }

    val density = LocalDensity.current
    var headerPx by remember { mutableIntStateOf(0) }
    val headerDp = with(density) { headerPx.toDp() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(XpTokens.Bg),
    ) {
        TopHalo()
        if (filtered.isEmpty()) {
            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(headerDp))
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val searching = searchOpen && q.isNotEmpty()
                    val (title, helper) = if (searching) "No matches for “$q”" to "Try a different search"
                        else emptyCopy(filterId, activeCategory?.name)
                    EmptyState(title, helper)
                }
            }
        } else Column(
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
                        onOpenNote = { id ->
                            if (selectMode) {
                                selected = if (id in selected) selected - id else selected + id
                            } else onOpenNote(id)
                        },
                        onLongPress = { row ->
                            selectMode = true
                            selected = selected + row.id
                        },
                        selected = item.row.id in selected,
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
                    activeColorHex = activeCategory?.colorHex,
                    onPick = { filterId = it },
                    onClear = { filterId = null },
                )
            }
            QuickEntryStrip(onCompose = onComposeQuick)
        }
        if (selectMode) {
            val visibleIds = visibleNotes.map { it.id }.toSet()
            SelectionBar(
                count = selected.size,
                allSelected = visibleIds.isNotEmpty() && selected.containsAll(visibleIds),
                onToggleAll = {
                    selected = if (selected.containsAll(visibleIds)) selected - visibleIds
                    else selected + visibleIds
                },
                onShare = {
                    val pairs = visibleNotes.filter { it.id in selected }
                        .map { it.title to it.preview }
                    if (pairs.isNotEmpty()) shareNotesAsMarkdown(context, pairs)
                },
                onDelete = { if (selected.isNotEmpty()) pendingBulkDelete = true },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp),
            )
        } else {
            XpFab(
                R.drawable.ic_plus, "New note", shadow = true,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 50.dp, bottom = 130.dp),
                onClick = { onNewNote(filterId ?: 0L) },
            )
        }
    }
    if (pendingBulkDelete) {
        val n = selected.size
        ConfirmDeleteDialog(
            title = if (n == 1) "Delete note?" else "Delete $n notes?",
            subject = if (n == 1)
                notesOnly.firstOrNull { it.id in selected }?.title?.ifBlank { "Untitled" } ?: "Untitled"
            else "$n notes",
            onCancel = { pendingBulkDelete = false },
            onConfirm = {
                selected.forEach(onDeleteNote)
                selected = emptySet()
                selectMode = false
                pendingBulkDelete = false
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

private fun emptyCopy(filterId: Long?, categoryName: String?): Pair<String, String> = when {
    filterId == null -> "No notes yet" to "Tap + to write your first"
    filterId == 0L -> "Nothing uncategorized" to "Notes without a category land here"
    else -> "Nothing in ${categoryName ?: "this category"}" to "Tap + to add the first"
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
            .padding(start = 22.dp, end = 18.dp),
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

