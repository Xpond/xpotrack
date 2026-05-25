package com.xpotrack.app.ui.tasks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xpotrack.app.ui.components.cutoutSafeTopPadding
import com.xpotrack.app.ui.components.styleFor
import com.xpotrack.app.ui.theme.XpTokens
import kotlinx.coroutines.launch

@Composable
fun TaskDetailScreen(
    vm: TaskDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenNote: (Int) -> Unit = {},
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val notesDraft by vm.notesDraft.collectAsStateWithLifecycle()
    val linkedNote by vm.linkedNote.collectAsStateWithLifecycle()
    val pickerQuery by vm.pickerQuery.collectAsStateWithLifecycle()
    val pickerResults by vm.pickerResults.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var linkOpen by remember { mutableStateOf(false) }
    val task = s.task ?: return run { Box(Modifier.fillMaxSize().background(XpTokens.Bg).cutoutSafeTopPadding()) {} }
    val style = styleFor(task.level)
    val saveAndBack: () -> Unit = { scope.launch { vm.saveNotesIfDirty(); onBack() } }
    BackHandler(onBack = saveAndBack)

    if (linkOpen) {
        LinkNoteDialog(
            results = pickerResults,
            query = pickerQuery,
            onQueryChange = vm::setPickerQuery,
            selectedId = task.linkedNoteId,
            onPick = { vm.setLinkedNote(it); linkOpen = false },
            onDismiss = { linkOpen = false },
        )
    }

    Column(Modifier.fillMaxSize().background(XpTokens.Bg).cutoutSafeTopPadding()) {
        TopBar(
            counter = if (s.indexToday > 0) "Task · ${s.indexToday} of ${s.totalToday} today" else "Task",
        )
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
            HeroTime(task, style)
            Spacer(Modifier.height(10.dp))
            Text(
                task.title, color = XpTokens.Ink, fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp, letterSpacing = (-0.02).em,
            )
            Spacer(Modifier.height(10.dp))
            NotesArea(value = notesDraft, enabled = !task.isDone, onChange = vm::onNotesChange)
            Spacer(Modifier.height(22.dp))
            FieldsCard(task, style, onAnyRow = { if (!task.isDone) onEdit(task.id) })
            Spacer(Modifier.height(18.dp))
            LinkedNoteSection(
                note = linkedNote,
                onPick = { linkOpen = true },
                onOpen = { linkedNote?.id?.let(onOpenNote) },
            )
            Spacer(Modifier.weight(1f))
            ActionRow(
                done = task.isDone,
                onMarkDone = { scope.launch { vm.saveNotesIfDirty(); vm.markDone(); onBack() } },
                onDelete = { scope.launch { vm.delete(); onBack() } },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
