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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.xpotrack.app.R
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun NotesListScreen(
    notes: List<NoteRow>,
    onOpenNote: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mode by remember { mutableStateOf(NotesMode.Category) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(XpTokens.Bg),
    ) {
        TopHalo()
        Column(Modifier.fillMaxSize()) {
            NotesHeader(notes = notes, mode = mode, onToggle = { mode = it })
            ModeStrip(notes = notes, mode = mode)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                if (mode == NotesMode.Category) NotesCategoryContent(notes, onOpenNote)
                else NotesChronoContent(notes, onOpenNote)
                Spacer(Modifier.height(100.dp))
            }
        }
        NotesFab(Modifier.align(Alignment.BottomEnd), onClick = { onOpenNote(0) })
    }
}

enum class NotesMode { Category, Chrono }

@Composable
private fun TopHalo() {
    // status-bar radial glow from system.jsx `.xp-app::before`
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
private fun NotesHeader(notes: List<NoteRow>, mode: NotesMode, onToggle: (NotesMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 18.dp, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Friday · May 16".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = XpTokens.Ink3,
            )
            Spacer(Modifier.height(8.dp))
            Text("Notes", style = MaterialTheme.typography.displayLarge, color = XpTokens.Ink)
        }
        IconBtn(R.drawable.ic_search, "Search", tint = XpTokens.Ink2)
        Spacer(Modifier.size(4.dp))
        val isChrono = mode == NotesMode.Chrono
        IconBtn(
            iconRes = if (isChrono) R.drawable.ic_grouped else R.drawable.ic_sort_date,
            contentDesc = "Toggle sort mode",
            tint = if (isChrono) XpTokens.Teal else XpTokens.Ink2,
            borderColor = if (isChrono) XpTokens.Teal else XpTokens.Hair2,
            background = if (isChrono) Color(0x0F5EEAD4) else Color.Transparent,
            onClick = { onToggle(if (isChrono) NotesMode.Category else NotesMode.Chrono) },
        )
    }
}

@Composable
private fun IconBtn(
    iconRes: Int,
    contentDesc: String,
    tint: Color,
    borderColor: Color = XpTokens.Hair2,
    background: Color = Color.Transparent,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(background)
            .border(0.5.dp, borderColor, CircleShape)
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
private fun ModeStrip(notes: List<NoteRow>, mode: NotesMode) {
    val text = when (mode) {
        NotesMode.Category -> "${Categories.count { c -> notes.any { it.category == c.name } }} categories · ${notes.size} notes"
        NotesMode.Chrono -> "${notes.size} notes · newest first"
    }
    Row(
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 8.dp),
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = XpTokens.Ink3,
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
