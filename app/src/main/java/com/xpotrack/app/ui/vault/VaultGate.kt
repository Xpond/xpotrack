package com.xpotrack.app.ui.vault

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xpotrack.app.XpApp

// The Vault tab is its own sub-router. State machine lives in VaultViewModel.
// VaultGate is what TabsScaffold renders for XpTab.Vault.

@Composable
fun VaultGate(onLockExit: () -> Unit) {
    val app = LocalContext.current.applicationContext as XpApp
    val vm: VaultViewModel = viewModel(
        factory = VaultViewModel.Factory(app.vaultRepo, app.vaultMeta, app.vaultSession),
    )
    val phase by vm.phase.collectAsStateWithLifecycle()
    val locked by vm.locked.collectAsStateWithLifecycle()

    LaunchedEffect(phase) { vm.touch() }

    when (val p = phase) {
        VaultPhase.Setup  -> VaultSetupScreen(vm)
        VaultPhase.Unlock -> VaultUnlockScreen(vm)
        VaultPhase.List   -> VaultListScreen(
            rows = locked,
            onOpen = { vm.openNote(it) },
            onNew = { vm.openNote(0L) },
            onLockNow = { vm.lockNow(); onLockExit() },
            onDelete = { vm.deleteRow(it) },
        )
        is VaultPhase.Note -> LockedNoteScreen(
            vm = vm, noteId = p.id, onBack = { vm.backToList() },
        )
    }
}
