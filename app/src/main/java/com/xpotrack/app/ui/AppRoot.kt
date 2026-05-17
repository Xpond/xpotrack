package com.xpotrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.xpotrack.app.ui.categories.CategoryManagerSheet
import com.xpotrack.app.ui.categories.CategoryManagerViewModel
import com.xpotrack.app.ui.categories.CategoryPickerSheet
import com.xpotrack.app.ui.components.XpBottomTabs
import com.xpotrack.app.ui.components.XpTab
import com.xpotrack.app.ui.more.MoreStubScreen
import com.xpotrack.app.ui.notes.NotesEditorScreen
import com.xpotrack.app.ui.notes.NotesEditorViewModel
import com.xpotrack.app.ui.notes.NotesListScreen
import com.xpotrack.app.ui.notes.NotesViewModel
import com.xpotrack.app.ui.quick.QuickNotesScreen
import com.xpotrack.app.ui.quick.QuickNotesViewModel
import com.xpotrack.app.ui.tasks.TaskCreateSheet
import com.xpotrack.app.ui.tasks.TaskCreateViewModel
import com.xpotrack.app.ui.tasks.TaskDetailScreen
import com.xpotrack.app.ui.tasks.TaskDetailViewModel
import com.xpotrack.app.ui.tasks.TasksTimelineScreen
import com.xpotrack.app.ui.tasks.TasksViewModel
import com.xpotrack.app.ui.theme.XpTokens
import com.xpotrack.app.ui.vault.VaultGate

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    var sheetTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    var sheetToken by rememberSaveable { mutableStateOf(0) }
    val openSheet: (Long) -> Unit = { id -> sheetToken += 1; sheetTaskId = id }

    var activeTab by rememberSaveable { mutableStateOf(XpTab.Notes) }

    // Picker + manager state — both live at the root so they can stack from any
    // tab/editor. Picker holds a lambda so it can't be saveable — losing it on
    // process death is fine.
    var pickerSelected by remember { mutableStateOf<Long?>(null) }
    var pickerOnPick by remember { mutableStateOf<((Long) -> Unit)?>(null) }
    var managerOpen by rememberSaveable { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(XpTokens.Bg)
            .systemBarsPadding(),
    ) {
        NavHost(navController = nav, startDestination = "tabs") {
            composable("tabs") {
                TabsScaffold(
                    active = activeTab,
                    onSelectTab = { activeTab = it },
                    onOpenNote = { id -> nav.navigate("editor/$id") },
                    onOpenTask = { id -> nav.navigate("task/$id") },
                    onNewTask = { openSheet(0L) },
                    onLockExit = { activeTab = XpTab.Notes },
                    onManageCategories = { managerOpen = true },
                    onOpenQuick = { nav.navigate("quick") },
                )
            }
            composable("quick") {
                val app = LocalContext.current.applicationContext as XpApp
                val vm: QuickNotesViewModel = viewModel(
                    key = "quick-notes",
                    factory = QuickNotesViewModel.Factory(app.quickNotesRepo),
                )
                QuickNotesScreen(vm = vm, onBack = { nav.popBackStack() })
            }
            composable(
                route = "editor/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType }),
            ) { entry ->
                val app = LocalContext.current.applicationContext as XpApp
                val id = entry.arguments?.getInt("id") ?: 0
                val vm: NotesEditorViewModel = viewModel(
                    key = "editor-$id",
                    factory = NotesEditorViewModel.Factory(app.notesRepo, app.categoryRepo, id),
                )
                NotesEditorScreen(
                    vm = vm, onBack = { nav.popBackStack() },
                    onPickCategory = {
                        pickerSelected = vm.state.value.categoryId
                        pickerOnPick = vm::setCategory
                    },
                )
            }
            composable(
                route = "task/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val app = LocalContext.current.applicationContext as XpApp
                val id = entry.arguments?.getLong("id") ?: 0L
                val vm: TaskDetailViewModel = viewModel(
                    key = "task-detail-$id",
                    factory = TaskDetailViewModel.Factory(app.tasksRepo, id),
                )
                // Refresh in place when the sheet closes — avoids re-keying the VM,
                // which would briefly flash an empty state during recreation.
                LaunchedEffect(sheetToken) { vm.refresh() }
                TaskDetailScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onEdit = { openSheet(it) },
                )
            }
        }

        sheetTaskId?.let { id ->
            val app = LocalContext.current.applicationContext as XpApp
            val vm: TaskCreateViewModel = viewModel(
                key = "task-create-$id-$sheetToken",
                factory = TaskCreateViewModel.Factory(app.tasksRepo, id),
            )
            TaskCreateSheet(
                vm = vm,
                onDismiss = {
                    sheetTaskId = null
                    (nav.currentBackStackEntry?.destination?.route ?: "")
                        .let { if (it.startsWith("task/")) sheetToken += 1 }
                },
            )
        }

        if (pickerSelected != null && pickerOnPick != null) {
            val app = LocalContext.current.applicationContext as XpApp
            val cats by app.categoryRepo.observeAll().collectAsStateWithLifecycle(emptyList())
            CategoryPickerSheet(
                categories = cats,
                selectedId = pickerSelected ?: 0L,
                onPick = { chosen ->
                    pickerOnPick?.invoke(chosen)
                    pickerSelected = null; pickerOnPick = null
                },
                onManage = { pickerSelected = null; pickerOnPick = null; managerOpen = true },
                onDismiss = { pickerSelected = null; pickerOnPick = null },
            )
        }

        if (managerOpen) {
            val app = LocalContext.current.applicationContext as XpApp
            val vm: CategoryManagerViewModel = viewModel(
                key = "category-manager",
                factory = CategoryManagerViewModel.Factory(app.categoryRepo),
            )
            CategoryManagerSheet(vm = vm, onDismiss = { managerOpen = false })
        }
    }
}

@Composable
private fun TabsScaffold(
    active: XpTab,
    onSelectTab: (XpTab) -> Unit,
    onOpenNote: (Int) -> Unit,
    onOpenTask: (Long) -> Unit,
    onNewTask: () -> Unit,
    onLockExit: () -> Unit,
    onManageCategories: () -> Unit,
    onOpenQuick: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as XpApp
    val notesVm: NotesViewModel = viewModel(factory = NotesViewModel.Factory(app.notesRepo, app.categoryRepo, app.quickNotesRepo))
    val tasksVm: TasksViewModel = viewModel(factory = TasksViewModel.Factory(app.tasksRepo))
    val notes by notesVm.notes.collectAsStateWithLifecycle()
    val tasks by tasksVm.tasks.collectAsStateWithLifecycle()
    val cats by notesVm.categories.collectAsStateWithLifecycle()
    val quick by notesVm.quickSummary.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (active) {
                XpTab.Notes -> NotesListScreen(
                    notes = notes, categories = cats, quick = quick,
                    onOpenNote = onOpenNote,
                    onManageCategories = onManageCategories,
                    onOpenQuick = onOpenQuick,
                )
                XpTab.Tasks -> TasksTimelineScreen(
                    tasks = tasks,
                    onOpenTask = { id -> if (id == 0L) onNewTask() else onOpenTask(id) },
                )
                XpTab.Vault -> VaultGate(onLockExit = onLockExit)
                XpTab.More  -> MoreStubScreen()
            }
        }
        XpBottomTabs(active = active, onSelect = onSelectTab)
    }
}
