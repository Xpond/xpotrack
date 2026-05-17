package com.xpotrack.app.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.xpotrack.app.ui.theme.XpTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerSheet(vm: CategoryManagerViewModel, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cats by vm.categories.collectAsStateWithLifecycle()
    val edit by vm.edit.collectAsStateWithLifecycle()
    val pendingDelete by vm.pendingDelete.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = XpTokens.Surface1,
        contentColor = XpTokens.Ink,
        dragHandle = { Grabber() },
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Header(onNew = vm::startCreate)
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                if (cats.isEmpty() && edit?.isNew != true) EmptyState()
                cats.forEachIndexed { i, c ->
                    if (edit?.id == c.id) EditRow(edit!!, vm)
                    else CategoryRow(c, first = i == 0,
                        onRename = { vm.startRename(c) }, onDelete = { vm.askDelete(c) })
                }
                if (edit?.isNew == true) EditRow(edit!!, vm, isNew = true)
                Spacer(Modifier.height(12.dp))
            }
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
    if (!first) Divider()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(10.dp).clip(CircleShape).background(parseHexColor(c.colorHex)))
        Spacer(Modifier.width(12.dp))
        Text(c.name, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = XpTokens.Ink,
            modifier = Modifier.weight(1f))
        IconBtn(R.drawable.ic_pencil, "Rename", onClick = onRename)
        Spacer(Modifier.width(2.dp))
        IconBtn(R.drawable.ic_trash, "Delete", onClick = onDelete)
    }
}

@Composable
private fun EditRow(edit: CategoryEdit, vm: CategoryManagerViewModel, isNew: Boolean = false) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(12.dp))
            .background(Color(0x0A5EEAD4))
            .border(0.5.dp, XpTokens.Teal, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
                .background(parseHexColor(edit.colorHex).copy(alpha = 0.15f))
                .border(0.5.dp, parseHexColor(edit.colorHex), RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = edit.name, onValueChange = vm::editName, singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = XpTokens.Teal, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(XpTokens.Teal),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (edit.name.isEmpty()) Text(
                        if (isNew) "New category" else "Name",
                        fontSize = 14.sp, color = XpTokens.Ink3,
                    )
                    inner()
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CategoryPalette.forEach { hex ->
                val sel = hex == edit.colorHex
                Box(
                    Modifier.size(22.dp).clip(CircleShape).background(parseHexColor(hex))
                        .let { if (sel) it.border(2.dp, XpTokens.Teal, CircleShape) else it }
                        .clickable { vm.editColor(hex) },
                )
            }
            Spacer(Modifier.weight(1f))
            Text("Cancel", fontSize = 12.5.sp, color = XpTokens.Ink3,
                modifier = Modifier.clickable(onClick = vm::cancelEdit).padding(horizontal = 8.dp, vertical = 4.dp))
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

@Composable
private fun Grabber() {
    Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(width = 38.dp, height = 4.dp).clip(RoundedCornerShape(2.dp))
            .background(XpTokens.Ink3.copy(alpha = 0.35f)))
    }
}

@Composable
private fun Divider() { Box(Modifier.fillMaxWidth().height(0.5.dp).background(XpTokens.Hair)) }

@Composable
private fun IconBtn(iconRes: Int, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(iconRes), desc, tint = XpTokens.Ink3, modifier = Modifier.size(14.dp))
    }
}
