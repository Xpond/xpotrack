package com.xpotrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xpotrack.app.XpApp
import com.xpotrack.app.ui.components.XpBottomTabs
import com.xpotrack.app.ui.components.XpTab
import com.xpotrack.app.ui.more.MoreStubScreen
import com.xpotrack.app.ui.notes.NotesListScreen
import com.xpotrack.app.ui.notes.NotesViewModel
import com.xpotrack.app.ui.tasks.TasksTimelineScreen
import com.xpotrack.app.ui.tasks.TasksViewModel
import com.xpotrack.app.ui.theme.XpTokens
import com.xpotrack.app.ui.vault.VaultStubScreen

@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as XpApp
    val notesVm: NotesViewModel = viewModel(factory = NotesViewModel.Factory(app.notesRepo))
    val tasksVm: TasksViewModel = viewModel(factory = TasksViewModel.Factory(app.tasksRepo))
    val notes by notesVm.notes.collectAsStateWithLifecycle()
    val tasks by tasksVm.tasks.collectAsStateWithLifecycle()

    var active by rememberSaveable { mutableStateOf(XpTab.Notes) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XpTokens.Bg)
            .systemBarsPadding(),
    ) {
        Box(Modifier.weight(1f)) {
            when (active) {
                XpTab.Notes -> NotesListScreen(notes = notes)
                XpTab.Tasks -> TasksTimelineScreen(tasks = tasks)
                XpTab.Vault -> VaultStubScreen()
                XpTab.More  -> MoreStubScreen()
            }
        }
        XpBottomTabs(active = active, onSelect = { active = it })
    }
}
