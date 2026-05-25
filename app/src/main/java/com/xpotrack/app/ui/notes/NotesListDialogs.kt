package com.xpotrack.app.ui.notes

import androidx.compose.runtime.Composable
import androidx.paging.compose.LazyPagingItems
import com.xpotrack.app.ui.components.ConfirmDeleteDialog
import com.xpotrack.app.ui.quick.QuickRow

@Composable
internal fun BulkDeleteDialog(
    selected: Set<Int>,
    notes: LazyPagingItems<FeedItem.Note>,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val n = selected.size
    val firstSelected = notes.itemSnapshotList.items.firstOrNull { it.row.id in selected }?.row
    ConfirmDeleteDialog(
        title = if (n == 1) "Delete note?" else "Delete $n notes?",
        subject = if (n == 1)
            firstSelected?.title?.ifBlank { "Untitled" } ?: "Untitled"
        else "$n notes",
        onCancel = onCancel,
        onConfirm = onConfirm,
    )
}

@Composable
internal fun QuickDeleteDialog(
    row: QuickRow,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmDeleteDialog(
        title = "Delete quick note?",
        subject = row.text.lineSequence().firstOrNull()?.take(60).orEmpty().ifBlank { "Untitled" },
        onCancel = onCancel,
        onConfirm = onConfirm,
    )
}
