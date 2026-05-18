package com.xpotrack.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.data.model.Category
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun NotesFilterBar(
    label: String?,
    totalCount: Int,
    categories: List<Category>,
    notes: List<NoteRow>,
    onPick: (Long?) -> Unit,
    onClear: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { menuOpen = true }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (label == null) {
                    Text("All notes", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = XpTokens.Ink2)
                    Spacer(Modifier.width(6.dp))
                    Text(totalCount.toString(), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = XpTokens.Ink3)
                } else {
                    Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = XpTokens.Teal)
                }
                Spacer(Modifier.width(4.dp))
                Text("▾", color = XpTokens.Ink3, fontSize = 11.sp)
            }
            FilterMenu(
                expanded = menuOpen,
                onDismiss = { menuOpen = false },
                categories = categories,
                notes = notes,
                onPick = { id -> onPick(id); menuOpen = false },
            )
        }
        if (label != null) {
            Spacer(Modifier.width(10.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onClear)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("×", color = XpTokens.Ink3, fontSize = 14.sp)
                Spacer(Modifier.width(4.dp))
                Text("CLEAR", style = MaterialTheme.typography.labelMedium, color = XpTokens.Ink3)
            }
        }
    }
}

@Composable
private fun FilterMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    categories: List<Category>,
    notes: List<NoteRow>,
    onPick: (Long?) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .background(XpTokens.Surface1)
            .border(0.5.dp, XpTokens.Hair2, RoundedCornerShape(10.dp)),
    ) {
        FilterMenuRow("All notes", notes.size, onClick = { onPick(null) })
        FilterMenuHeader("BUILT-IN")
        val uncatCount = notes.count { it.categoryId == 0L }
        FilterMenuRow("Uncategorized", uncatCount, onClick = { onPick(0L) })
        if (categories.isNotEmpty()) {
            FilterMenuHeader("CUSTOM")
            categories.forEach { cat ->
                val c = notes.count { it.categoryId == cat.id }
                FilterMenuRow(cat.name, c, onClick = { onPick(cat.id) })
            }
        }
    }
}

@Composable
private fun FilterMenuHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = XpTokens.Ink3,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
    )
}

@Composable
private fun FilterMenuRow(name: String, count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = XpTokens.Ink)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(24.dp))
        Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = XpTokens.Ink3)
    }
}
