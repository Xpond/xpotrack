package com.xpotrack.app.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xpotrack.app.R
import com.xpotrack.app.data.model.Category
import com.xpotrack.app.ui.components.XpIconBtn
import com.xpotrack.app.ui.theme.XpTokens

// Manager content lives inside CategorySheet, so it does not own a
// ModalBottomSheet of its own. onCreated fires once per newly-created
// category so the caller (AppRoot) can auto-apply it to the editor.
@Composable
internal fun ManagerContent(
    vm: CategoryManagerViewModel,
    onCreated: (Long) -> Unit,
) {
    val cats by vm.categories.collectAsStateWithLifecycle()
    val edit by vm.edit.collectAsStateWithLifecycle()
    val pendingDelete by vm.pendingDelete.collectAsStateWithLifecycle()
    val lastCreated by vm.lastCreated.collectAsStateWithLifecycle()

    LaunchedEffect(lastCreated) {
        if (lastCreated > 0L) {
            val id = lastCreated
            vm.clearLastCreated()
            onCreated(id)
        }
    }

    // Reset any half-finished editor state on dismount so reopening the
    // manager from a fresh chip-tap never shows a leftover edit row or a
    // pending delete dialog from a previous session.
    DisposableEffect(Unit) {
        onDispose {
            vm.cancelEdit()
            vm.cancelDelete()
            vm.clearLastCreated()
        }
    }

    val scrollState = rememberScrollState()

    Column(Modifier.fillMaxWidth().imePadding().padding(bottom = 24.dp)) {
        Header(onNew = vm::startCreate)
        Column(
            Modifier.fillMaxWidth().heightIn(max = 360.dp)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
        ) {
            if (cats.isEmpty() && edit?.isNew != true) EmptyState()
            cats.forEachIndexed { i, c ->
                if (edit?.id == c.id) EditRow(edit!!, vm)
                else CategoryRow(c, first = i == 0,
                    onRename = { vm.startRename(c) }, onDelete = { vm.askDelete(c) })
            }
            Spacer(Modifier.height(12.dp))
        }
        // New-category editor sits below the list so it never slides under the
        // keyboard. imePadding above lifts the whole column so it stays reachable.
        if (edit?.isNew == true) {
            Box(Modifier.padding(horizontal = 16.dp)) { EditRow(edit!!, vm, isNew = true) }
        }
    }

    pendingDelete?.let { p ->
        DeleteDialog(p = p, onConfirm = vm::confirmDelete, onCancel = vm::cancelDelete)
    }
}

@Composable
private fun Header(onNew: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 22.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("Manage".uppercase(), style = MaterialTheme.typography.labelMedium, color = XpTokens.Ink3)
            Spacer(Modifier.height(6.dp))
            Text("Categories", color = XpTokens.Ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(
            Modifier.clip(CircleShape).background(XpTokens.Teal)
                .clickable(onClick = onNew).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_plus), null, tint = XpTokens.OnTeal, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text("New", color = XpTokens.OnTeal, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 24.dp)) {
        Text(
            "No categories yet. Tap New to create one.",
            color = XpTokens.Ink3, fontSize = 13.sp,
        )
    }
}

@Composable
private fun CategoryRow(c: Category, first: Boolean, onRename: () -> Unit, onDelete: () -> Unit) {
    if (!first) SheetDivider()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(10.dp).clip(CircleShape).background(parseHexColor(c.colorHex)))
        Spacer(Modifier.width(12.dp))
        Text(c.name, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = XpTokens.Ink,
            modifier = Modifier.weight(1f))
        XpIconBtn(R.drawable.ic_pencil, "Rename", tint = XpTokens.Ink3, size = 30.dp, iconSize = 14.dp, onClick = onRename)
        Spacer(Modifier.width(2.dp))
        XpIconBtn(R.drawable.ic_trash, "Delete", tint = XpTokens.Ink3, size = 30.dp, iconSize = 14.dp, onClick = onDelete)
    }
}

@Composable
private fun EditRow(edit: CategoryEdit, vm: CategoryManagerViewModel, isNew: Boolean = false) {
    val editColor = parseHexColor(edit.colorHex)
    Column(
        Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(12.dp))
            .background(Color(0x0A5EEAD4))
            .border(0.5.dp, XpTokens.Teal, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        // Wheel on the left; name + hex stack on the right reuses its height.
        Row(verticalAlignment = Alignment.CenterVertically) {
            HueRing(hex = edit.colorHex, onChange = vm::editColor, modifier = Modifier.size(64.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                val nameStyle = TextStyle(
                    fontSize = 14.sp, lineHeight = 20.sp,
                    color = XpTokens.Teal, fontWeight = FontWeight.Medium,
                )
                BasicTextField(
                    value = edit.name, onValueChange = vm::editName, singleLine = true,
                    textStyle = nameStyle,
                    cursorBrush = SolidColor(XpTokens.Teal),
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(XpTokens.Surface1)
                        .border(0.5.dp, editColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .heightIn(min = 36.dp)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (edit.name.isEmpty()) Text(
                                if (isNew) "New category" else "Name",
                                style = nameStyle.copy(color = XpTokens.Ink3),
                            )
                            inner()
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
                HexField(hex = edit.colorHex, onChange = vm::editColor, modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            Text("Cancel", fontSize = 12.5.sp, color = XpTokens.Ink3,
                modifier = Modifier.clickable(onClick = vm::cancelEdit).padding(horizontal = 8.dp, vertical = 4.dp))
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.clip(CircleShape).background(XpTokens.Teal)
                    .clickable(onClick = vm::commitEdit).padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(if (isNew) "Create" else "Save",
                    fontSize = 12.5.sp, color = XpTokens.OnTeal, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

