package com.xpotrack.app.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.xpotrack.app.R
import com.xpotrack.app.data.model.Category
import com.xpotrack.app.data.repo.NotesRepository
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
    notes: LazyPagingItems<FeedItem.Note>,
    quicks: List<FeedItem.Quick>,
    categories: List<Category>,
    counts: NotesRepository.Counts,
    filterId: Long?,
    onSetFilter: (Long?) -> Unit,
    query: String,
    onSetQuery: (String) -> Unit,
    onOpenNote: (Int) -> Unit,
    onNewNote: (Long) -> Unit,
    onComposeQuick: () -> Unit,
    onOpenQuickNote: (Long) -> Unit,
    onKeepQuick: (Long) -> Unit,
    onDeleteQuick: (Long) -> Unit,
    headerPx: Int,
    onHeaderPx: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onDeleteNote: (Int) -> Unit = {},
) {
    var searchOpen by remember { mutableStateOf(false) }
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

    // Quicks are filtered in-UI: they belong to "All" only and don't participate
    // in category filters. Search applies to both quicks (in-memory) and notes
    // (via DAO LIKE, routed through the VM).
    val q = query.trim()
    val visibleQuicks: List<FeedItem.Quick> = when {
        filterId != null -> emptyList()
        searchOpen && q.isNotEmpty() -> quicks.filter { it.row.text.contains(q, ignoreCase = true) }
        else -> quicks
    }

    // "Loaded" notes — the currently materialized pages. Used for select-all
    // and bulk-share, which can only operate on what the user can actually see.
    val loadedNoteIds: List<Int> = remember(notes.itemSnapshotList) {
        notes.itemSnapshotList.items.map { it.row.id }
    }

    val density = LocalDensity.current
    val headerDp = with(density) { headerPx.toDp() }

    val listState = rememberLazyListState()

    // Snap to top when the first loaded note changes (e.g., editor returns with
    // a new note). Guarded on the user being near the top so we don't yank them
    // out of a scrolled position.
    val topNoteId = notes.itemSnapshotList.items.firstOrNull()?.row?.id
    val topQuickId = visibleQuicks.firstOrNull()?.row?.id
    LaunchedEffect(topNoteId, topQuickId) {
        if (listState.firstVisibleItemIndex <= 1) listState.scrollToItem(0, 0)
    }

    val emptyAfterLoad = visibleQuicks.isEmpty() &&
        notes.itemCount == 0 &&
        notes.loadState.refresh !is LoadState.Loading

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(XpTokens.Bg),
    ) {
        TopHalo()
        if (emptyAfterLoad) {
            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(headerDp))
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val searching = searchOpen && q.isNotEmpty()
                    val (title, helper) = if (searching) "No matches for “$q”" to "Try a different search"
                        else emptyCopy(filterId, activeCategory?.name)
                    EmptyState(title, helper)
                }
            }
        } else LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = headerDp, bottom = 100.dp),
        ) {
            items(visibleQuicks, key = { "q${it.row.id}" }) { item ->
                val isLast = item == visibleQuicks.last() && notes.itemCount == 0
                QuickNoteEntry(
                    row = item.row,
                    isLast = isLast,
                    onKeep = { onKeepQuick(item.row.id) },
                    onOpen = { onOpenQuickNote(item.row.id) },
                    onLongPress = { pendingDeleteQuick = item.row },
                )
            }
            items(
                count = notes.itemCount,
                key = notes.itemKey { "n${it.row.id}" },
            ) { i ->
                val item = notes[i]
                val isLast = i == notes.itemCount - 1
                if (item != null) {
                    ChronoNoteRow(
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
                }
            }
        }
        PinnedHeader(onSize = { onHeaderPx(it.height) }) {
            if (searchOpen) {
                NotesSearchBar(
                    query = query,
                    onQueryChange = onSetQuery,
                    onClose = { searchOpen = false; onSetQuery("") },
                )
            } else {
                NotesHeader(onSearch = { searchOpen = true })
                NotesFilterBar(
                    label = filterLabel,
                    categories = categories,
                    counts = counts,
                    activeColorHex = activeCategory?.colorHex,
                    onPick = onSetFilter,
                    onClear = { onSetFilter(null) },
                )
            }
            QuickEntryStrip(onCompose = onComposeQuick)
        }
        if (selectMode) {
            val loadedIdSet = loadedNoteIds.toSet()
            SelectionBar(
                count = selected.size,
                allSelected = loadedIdSet.isNotEmpty() && selected.containsAll(loadedIdSet),
                onToggleAll = {
                    selected = if (selected.containsAll(loadedIdSet)) selected - loadedIdSet
                    else selected + loadedIdSet
                },
                onShare = {
                    val pairs = notes.itemSnapshotList.items
                        .filter { it.row.id in selected }
                        .map { it.row.title to it.row.preview }
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
        val firstSelected = notes.itemSnapshotList.items.firstOrNull { it.row.id in selected }?.row
        ConfirmDeleteDialog(
            title = if (n == 1) "Delete note?" else "Delete $n notes?",
            subject = if (n == 1)
                firstSelected?.title?.ifBlank { "Untitled" } ?: "Untitled"
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
