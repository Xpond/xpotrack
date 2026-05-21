package com.xpotrack.app.ui.vault

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R
import com.xpotrack.app.ui.components.ConfirmDeleteDialog
import com.xpotrack.app.ui.components.XpFab
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun VaultListScreen(
    rows: List<LockedNoteRow>,
    onOpen: (Long) -> Unit,
    onNew: () -> Unit,
    onLockNow: () -> Unit,
    onDelete: (Long) -> Unit = {},
) {
    var pendingDelete by remember { mutableStateOf<LockedNoteRow?>(null) }
    Box(Modifier.fillMaxSize().background(XpTokens.Bg)) {
        if (rows.isEmpty()) {
            Column(Modifier.fillMaxSize()) {
                Header(onLockNow = onLockNow)
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    EmptyState()
                }
                Footer()
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Header(onLockNow = onLockNow)
                Spacer(Modifier.height(12.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 100.dp)) {
                    rows.forEachIndexed { i, row ->
                        LockedRow(
                            row,
                            onClick = { onOpen(row.id) },
                            onLongClick = { pendingDelete = row },
                        )
                        if (i < rows.size - 1) {
                            Box(Modifier.fillMaxWidth().padding(horizontal = 22.dp).height(0.5.dp).background(XpTokens.Hair))
                        }
                    }
                }
                Footer()
            }
        }
        Fab(onClick = onNew)
    }
    pendingDelete?.let { row ->
        ConfirmDeleteDialog(
            title = "Delete locked note?",
            subject = row.title.ifBlank { "Untitled" },
            onCancel = { pendingDelete = null },
            onConfirm = {
                onDelete(row.id)
                pendingDelete = null
            },
        )
    }
}

@Composable
private fun Header(onLockNow: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_lock), null, tint = XpTokens.TealDim, modifier = Modifier.size(11.dp))
                Spacer(Modifier.size(6.dp))
                Text("VAULT · UNLOCKED", style = MaterialTheme.typography.labelSmall, color = XpTokens.TealDim)
            }
            Spacer(Modifier.height(8.dp))
            Text("Locked notes", style = MaterialTheme.typography.displayLarge, color = XpTokens.Ink)
        }
        Box(
            Modifier.size(38.dp).clip(CircleShape)
                .background(XpTokens.TealTint)
                .border(0.5.dp, XpTokens.Teal, CircleShape)
                .clickable(onClick = onLockNow),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_lock), "Lock now", tint = XpTokens.Teal, modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LockedRow(row: LockedNoteRow, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(R.drawable.ic_lock), null,
            tint = XpTokens.TealDim, modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.size(10.dp))
        Text(
            row.title.ifBlank { "Untitled" },
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
            color = XpTokens.Ink,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.size(14.dp))
        Text(
            row.when_,
            style = MaterialTheme.typography.labelSmall, color = XpTokens.Ink3,
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No locked notes", color = XpTokens.Ink2, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text("Tap + to add one", color = XpTokens.Ink3, fontSize = 12.sp)
    }
}

@Composable
private fun Footer() {
    Box(
        Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("Encrypted on this device. Never synced.", color = XpTokens.Ink3, fontSize = 11.sp)
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.Fab(onClick: () -> Unit) {
    XpFab(R.drawable.ic_plus, "New locked note", shadow = true, onClick = onClick,
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 42.dp, bottom = 86.dp))
}
