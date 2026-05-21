package com.xpotrack.app.ui.vault

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
    val context = LocalContext.current
    val app = context.applicationContext as XpApp
    val vm: VaultViewModel = viewModel(
        factory = VaultViewModel.Factory(app.vaultRepo, app.vaultMeta, app.vaultSession),
    )
    val phase by vm.phase.collectAsStateWithLifecycle()
    val locked by vm.locked.collectAsStateWithLifecycle()

    LaunchedEffect(phase) { vm.touch() }

    // Block screenshots, screen recording, and the recents thumbnail while the
    // Vault tab is on screen. Cleared on leaving so notes/tasks remain unaffected.
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

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
