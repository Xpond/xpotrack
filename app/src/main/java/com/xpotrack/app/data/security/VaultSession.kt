package com.xpotrack.app.data.security

import javax.crypto.SecretKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// In-memory vault state. The derived AES key lives here while the vault is open;
// dropping back to Locked clears it. Auto-lock fires after AUTO_LOCK_MS of inactivity.
//
// Callers must invoke touch() on user activity inside the vault (screen open, edit,
// scroll-on-resume) to reset the countdown. The screens own that wiring.

class VaultSession {

    sealed interface State {
        data object Locked : State
        data class Unlocked(val key: SecretKey, val unlockedAt: Long) : State
    }

    private val _state = MutableStateFlow<State>(State.Locked)
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timer: Job? = null

    fun unlock(key: SecretKey) {
        _state.value = State.Unlocked(key, System.currentTimeMillis())
        restartTimer()
    }

    fun lock() {
        timer?.cancel()
        timer = null
        _state.value = State.Locked
    }

    fun touch() {
        if (_state.value is State.Unlocked) restartTimer()
    }

    fun key(): SecretKey? = (_state.value as? State.Unlocked)?.key

    private fun restartTimer() {
        timer?.cancel()
        timer = scope.launch {
            delay(AUTO_LOCK_MS)
            lock()
        }
    }

    companion object {
        const val AUTO_LOCK_MS = 60_000L
    }
}
