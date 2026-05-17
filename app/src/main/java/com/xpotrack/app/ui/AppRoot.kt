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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xpotrack.app.XpApp
import com.xpotrack.app.ui.components.XpBottomTabs
import com.xpotrack.app.ui.components.XpTab
import com.xpotrack.app.ui.more.MoreStubScreen
import com.xpotrack.app.ui.notes.NotesEditorScreen
import com.xpotrack.app.ui.notes.NotesEditorViewModel
import com.xpotrack.app.ui.notes.NotesListScreen
import com.xpotrack.app.ui.notes.NotesViewModel
import com.xpotrack.app.ui.tasks.TaskCreateSheet
import com.xpotrack.app.ui.tasks.TaskCreateViewModel
import com.xpotrack.app.ui.tasks.TasksTimelineScreen
import com.xpotrack.app.ui.tasks.TasksViewModel
import com.xpotrack.app.ui.theme.XpTokens
import com.xpotrack.app.ui.vault.VaultStubScreen

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    Box(
        Modifier
            .fillMaxSize()
            .background(XpTokens.Bg)
            .systemBarsPadding(),
    ) {
        NavHost(navController = nav, startDestination = "tabs") {
            composable("tabs") {
                TabsScaffold(onOpenNote = { id -> nav.navigate("editor/$id") })
            }
            composable(
                route = "editor/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType }),
            ) { entry ->
                val app = LocalContext.current.applicationContext as XpApp
                val id = entry.arguments?.getInt("id") ?: 0
                val vm: NotesEditorViewModel = viewModel(
                    key = "editor-$id",
                    factory = NotesEditorViewModel.Factory(app.notesRepo, id),
                )
                NotesEditorScreen(vm = vm, onBack = { nav.popBackStack() })
            }
        }
    }
}

@Composable
private fun TabsScaffold(onOpenNote: (Int) -> Unit) {
    val app = LocalContext.current.applicationContext as XpApp
    val notesVm: NotesViewModel = viewModel(factory = NotesViewModel.Factory(app.notesRepo))
    val tasksVm: TasksViewModel = viewModel(factory = TasksViewModel.Factory(app.tasksRepo))
    val notes by notesVm.notes.collectAsStateWithLifecycle()
    val tasks by tasksVm.tasks.collectAsStateWithLifecycle()

    var active by rememberSaveable { mutableStateOf(XpTab.Notes) }
    var sheetTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    var sheetToken by rememberSaveable { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (active) {
                XpTab.Notes -> NotesListScreen(notes = notes, onOpenNote = onOpenNote)
                XpTab.Tasks -> TasksTimelineScreen(
                    tasks = tasks,
                    onOpenTask = {
                        sheetToken += 1
                        sheetTaskId = it
                    },
                )
                XpTab.Vault -> VaultStubScreen()
                XpTab.More  -> MoreStubScreen()
            }
        }
        XpBottomTabs(active = active, onSelect = { active = it })
    }

    sheetTaskId?.let { id ->
        val vm: TaskCreateViewModel = viewModel(
            key = "task-create-$id-$sheetToken",
            factory = TaskCreateViewModel.Factory(app.tasksRepo, id),
        )
        TaskCreateSheet(vm = vm, onDismiss = { sheetTaskId = null })
    }
}
