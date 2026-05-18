package com.xpotrack.app.ui.vault

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R
import com.xpotrack.app.ui.components.ConfirmDeleteDialog
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
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 100.dp)) {
            Header(onLockNow = onLockNow)
            Spacer(Modifier.height(12.dp))
            if (rows.isEmpty()) EmptyState() else Column(Modifier.padding(horizontal = 16.dp)) {
                rows.forEachIndexed { i, row ->
                    LockedRow(
                        row,
                        onClick = { onOpen(row.id) },
                        onLongClick = { pendingDelete = row },
                    )
                    if (i < rows.size - 1) Box(Modifier.fillMaxWidth().height(0.5.dp).background(XpTokens.Hair))
                }
            }
            Footer()
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
        Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Bottom,
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
                .background(XpTokens.TealTint)
                .border(0.5.dp, XpTokens.Hair2, RoundedCornerShape(8.dp))
                .padding(top = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_lock), null, tint = XpTokens.TealDim, modifier = Modifier.size(13.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.category.uppercase(),
                    style = MaterialTheme.typography.labelSmall, color = XpTokens.TealDim,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    row.when_,
                    fontSize = 11.sp, color = XpTokens.Ink3,
                    fontFamily = com.xpotrack.app.ui.theme.GeistMono,
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                row.title, color = XpTokens.Ink, fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = (-0.15).sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "•••• ••••",
                color = XpTokens.Ink2, fontSize = 13.sp,
                fontFamily = com.xpotrack.app.ui.theme.GeistMono, letterSpacing = 0.5.sp,
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(painterResource(R.drawable.ic_lock), null, tint = XpTokens.Ink3, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(12.dp))
        Text("No locked notes yet", color = XpTokens.Ink2, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "Tap + to add one. The body is encrypted on this device.",
            color = XpTokens.Ink3, fontSize = 12.sp,
        )
    }
}

@Composable
private fun Footer() {
    Row(
        Modifier.padding(horizontal = 22.dp).fillMaxWidth()
            .padding(top = 24.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_shield), null, tint = XpTokens.Ink3, modifier = Modifier.size(14.dp))
        Spacer(Modifier.size(10.dp))
        Column {
            Text("Encrypted on this device. Never synced.", color = XpTokens.Ink3, fontSize = 11.sp)
            Text("Unlocks with fingerprint or passphrase.", color = XpTokens.Ink3, fontSize = 11.sp)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.Fab(onClick: () -> Unit) {
    Box(
        Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 22.dp)
            .size(56.dp).clip(CircleShape).background(XpTokens.Teal)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_plus), "New locked note", tint = XpTokens.OnTeal, modifier = Modifier.size(22.dp))
    }
}
