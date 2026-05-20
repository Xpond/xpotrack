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
import com.xpotrack.app.ui.categories.CategoryManagerViewModel
import com.xpotrack.app.ui.categories.CategoryRequest
import com.xpotrack.app.ui.categories.CategorySheet
import com.xpotrack.app.ui.categories.CategorySheetMode
import com.xpotrack.app.ui.components.XpBottomTabs
import com.xpotrack.app.ui.components.XpTab
import com.xpotrack.app.ui.settings.SettingsScreen
import com.xpotrack.app.ui.notes.NotesEditorScreen
import com.xpotrack.app.ui.notes.NotesEditorViewModel
import com.xpotrack.app.ui.notes.NotesListScreen
import com.xpotrack.app.ui.notes.NotesViewModel
import com.xpotrack.app.ui.quick.QuickEditorScreen
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
    // Date the create sheet should default to when opening a brand-new task.
    // Ignored when editing existing tasks (id != 0L) since the row carries its own.
    var sheetInitialDate by rememberSaveable { mutableStateOf(0L) }
    var sheetToken by rememberSaveable { mutableStateOf(0) }
    val openSheet: (Long) -> Unit = { id -> sheetToken += 1; sheetTaskId = id }
    val openNewSheet: (Long) -> Unit = { date ->
        sheetInitialDate = date; sheetToken += 1; sheetTaskId = 0L
    }

    var activeTab by rememberSaveable { mutableStateOf(XpTab.Notes) }

    // Picker/manager state lives here so the sheet can stack from any tab or
    // editor. `categoryRequest` bundles the caller's selected id and apply
    // callback into one atomic state write — opening the sheet must flip the
    // predicate exactly once, otherwise the sheet's show animation can race
    // with the parent recomposition and settle half-open. Non-saveable because
    // the callback can't survive process death; that's fine.
    var categoryRequest by remember { mutableStateOf<CategoryRequest?>(null) }
    var sheetMode by remember { mutableStateOf(CategorySheetMode.Picker) }

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
                    onNewTask = { date -> openNewSheet(date) },
                    onLockExit = { activeTab = XpTab.Notes },
                    onComposeQuick = { nav.navigate("quick/edit/0") },
                    onOpenQuickNote = { id -> nav.navigate("quick/edit/$id") },
                )
            }
            composable(
                route = "quick/edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val app = LocalContext.current.applicationContext as XpApp
                val id = entry.arguments?.getLong("id") ?: 0L
                val vm: QuickNotesViewModel = viewModel(
                    key = "quick-notes",
                    factory = QuickNotesViewModel.Factory(app.quickNotesRepo),
                )
                QuickEditorScreen(
                    vm = vm,
                    noteId = id,
                    onBack = { nav.popBackStack() },
                )
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
                        sheetMode = CategorySheetMode.Picker
                        categoryRequest = CategoryRequest(
                            selectedId = vm.state.value.categoryId,
                            onApply = vm::setCategory,
                        )
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
                    factory = TaskDetailViewModel.Factory(app.tasksRepo, app.notesRepo, id),
                )
                // Refresh in place when the sheet closes — avoids re-keying the VM,
                // which would briefly flash an empty state during recreation.
                LaunchedEffect(sheetToken) { vm.refresh() }
                TaskDetailScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onEdit = { openSheet(it) },
                    onOpenNote = { noteId -> nav.navigate("editor/$noteId") },
                )
            }
        }

        sheetTaskId?.let { id ->
            val app = LocalContext.current.applicationContext as XpApp
            val vm: TaskCreateViewModel = viewModel(
                key = "task-create-$id-$sheetToken",
                factory = TaskCreateViewModel.Factory(app.tasksRepo, app.notesRepo, id, sheetInitialDate),
            )
            val allTasks by app.tasksRepo.observeAll().collectAsStateWithLifecycle(emptyList())
            val datesWithTasks = remember(allTasks) {
                allTasks.asSequence().map { it.dateEpochDay }.filter { it > 0L }.toSet()
            }
            TaskCreateSheet(
                vm = vm,
                datesWithTasks = datesWithTasks,
                onDismiss = {
                    sheetTaskId = null
                    (nav.currentBackStackEntry?.destination?.route ?: "")
                        .let { if (it.startsWith("task/")) sheetToken += 1 }
                },
            )
        }

        // One sheet, two modes. The picker→manager transition cross-fades the
        // inner content inside the same ModalBottomSheet — no double-scrim and
        // no mount/unmount race. All dismiss paths animate hide() before
        // clearing categoryRequest, so the slide-down always plays in full.
        val app = LocalContext.current.applicationContext as XpApp
        val cats by app.categoryRepo.observeAll().collectAsStateWithLifecycle(emptyList())
        val managerVm: CategoryManagerViewModel = viewModel(
            key = "category-manager",
            factory = CategoryManagerViewModel.Factory(app.categoryRepo),
        )
        CategorySheet(
            visible = categoryRequest != null,
            mode = sheetMode,
            categories = cats,
            selectedId = categoryRequest?.selectedId ?: 0L,
            managerVm = managerVm,
            onPick = { chosen ->
                categoryRequest?.onApply?.invoke(chosen)
                categoryRequest = null
                sheetMode = CategorySheetMode.Picker
            },
            onManage = { sheetMode = CategorySheetMode.Manager },
            onCreated = { newId ->
                categoryRequest?.onApply?.invoke(newId)
                categoryRequest = null
                sheetMode = CategorySheetMode.Picker
            },
            onDismiss = {
                categoryRequest = null
                sheetMode = CategorySheetMode.Picker
            },
        )
    }
}

@Composable
private fun TabsScaffold(
    active: XpTab,
    onSelectTab: (XpTab) -> Unit,
    onOpenNote: (Int) -> Unit,
    onOpenTask: (Long) -> Unit,
    onNewTask: (Long) -> Unit,
    onLockExit: () -> Unit,
    onComposeQuick: () -> Unit,
    onOpenQuickNote: (Long) -> Unit,
) {
    val app = LocalContext.current.applicationContext as XpApp
    val notesVm: NotesViewModel = viewModel(factory = NotesViewModel.Factory(app.notesRepo, app.categoryRepo, app.quickNotesRepo))
    val tasksVm: TasksViewModel = viewModel(factory = TasksViewModel.Factory(app.tasksRepo))
    val notes by notesVm.notes.collectAsStateWithLifecycle()
    val quicks by notesVm.quicks.collectAsStateWithLifecycle()
    val tasks by tasksVm.tasks.collectAsStateWithLifecycle()
    val cats by notesVm.categories.collectAsStateWithLifecycle()
    val selectedDate by tasksVm.selectedDate.collectAsStateWithLifecycle()
    val datesWithTasks by tasksVm.datesWithTasks.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (active) {
                XpTab.Notes -> NotesListScreen(
                    notes = notes, quicks = quicks, categories = cats,
                    onOpenNote = onOpenNote,
                    onComposeQuick = onComposeQuick,
                    onOpenQuickNote = onOpenQuickNote,
                    onKeepQuick = notesVm::keepQuick,
                    onDeleteQuick = notesVm::deleteQuick,
                    onDeleteNote = notesVm::delete,
                )
                XpTab.Tasks -> TasksTimelineScreen(
                    tasks = tasks,
                    selectedDate = selectedDate,
                    datesWithTasks = datesWithTasks,
                    onSelectDate = tasksVm::setSelectedDate,
                    onOpenTask = { id -> if (id == 0L) onNewTask(selectedDate) else onOpenTask(id) },
                    onDeleteTask = tasksVm::delete,
                )
                XpTab.Vault -> VaultGate(onLockExit = onLockExit)
                XpTab.More  -> SettingsScreen()
            }
        }
        XpBottomTabs(active = active, onSelect = onSelectTab)
    }
}
