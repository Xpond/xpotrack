package com.xpotrack.app.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xpotrack.app.data.repo.VaultRepository
import com.xpotrack.app.data.security.VaultCrypto
import com.xpotrack.app.data.security.VaultKeyStore
import com.xpotrack.app.data.security.VaultMetaStore
import com.xpotrack.app.data.security.VaultSession
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface VaultPhase {
    data object Setup : VaultPhase
    data object Unlock : VaultPhase
    data object List : VaultPhase
    data class Note(val id: Long) : VaultPhase
}

class VaultViewModel(
    private val repo: VaultRepository,
    private val meta: VaultMetaStore,
    private val session: VaultSession,
) : ViewModel() {

    private val _phase = MutableStateFlow<VaultPhase>(initialPhase())
    val phase: StateFlow<VaultPhase> = _phase.asStateFlow()

    val locked: StateFlow<List<LockedNoteRow>> =
        repo.observeLocked().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unlockError: MutableStateFlow<String?> = MutableStateFlow(null)
    val verifying: MutableStateFlow<Boolean> = MutableStateFlow(false)

    // Recomputes phase whenever the session state flips (auto-lock fires, etc.).
    init {
        viewModelScope.launch {
            session.state.collect { s ->
                when (s) {
                    is VaultSession.State.Locked -> if (_phase.value !is VaultPhase.Setup) {
                        _phase.value = if (meta.isSetup()) VaultPhase.Unlock else VaultPhase.Setup
                    }
                    is VaultSession.State.Unlocked -> if (_phase.value is VaultPhase.Unlock || _phase.value is VaultPhase.Setup) {
                        _phase.value = VaultPhase.List
                    }
                }
            }
        }
    }

    private fun initialPhase(): VaultPhase = when {
        !meta.isSetup() -> VaultPhase.Setup
        session.state.value is VaultSession.State.Unlocked -> VaultPhase.List
        else -> VaultPhase.Unlock
    }

    fun touch() = session.touch()

    fun hasBiometric(): Boolean = meta.hasBiometric()

    fun biometricIv(): ByteArray = meta.biometricIv()

    // --- Setup ---

    fun setupPassphrase(pass: CharArray, enableBiometric: Boolean, biometricCipher: Cipher? = null) {
        if (pass.size < 6) { unlockError.value = "Passphrase must be at least 6 characters"; return }
        unlockError.value = null
        verifying.value = true
        viewModelScope.launch {
            val salt = VaultCrypto.randomBytes(VaultCrypto.SALT_BYTES)
            val (verifier, key) = withContext(Dispatchers.Default) {
                VaultCrypto.deriveVerifier(pass, salt) to VaultCrypto.deriveKey(pass, salt)
            }
            meta.saveSetup(salt, verifier)
            if (enableBiometric && biometricCipher != null) {
                // Wrap the derived AES key, not the passphrase. Skips PBKDF2 on unlock.
                val (ct, iv) = VaultKeyStore.wrap(biometricCipher, key.encoded)
                meta.saveBiometric(ct, iv)
            }
            session.unlock(key)
            verifying.value = false
        }
    }

    // --- Unlock paths ---

    fun unlockWithPassphrase(pass: CharArray) {
        unlockError.value = null
        verifying.value = true
        viewModelScope.launch {
            val salt = meta.salt()
            val candidate = withContext(Dispatchers.Default) { VaultCrypto.deriveVerifier(pass, salt) }
            if (!VaultCrypto.constantTimeEquals(candidate, meta.verifier())) {
                unlockError.value = "Wrong passphrase"
                verifying.value = false
                return@launch
            }
            val key = withContext(Dispatchers.Default) { VaultCrypto.deriveKey(pass, salt) }
            session.unlock(key)
            verifying.value = false
        }
    }

    fun unlockWithBiometric(cipher: Cipher) {
        verifying.value = true
        viewModelScope.launch {
            // Wrapped blob is the 32-byte AES key itself — no PBKDF2 needed.
            val keyBytes = VaultKeyStore.unwrap(cipher, meta.biometricBlob())
            val key = SecretKeySpec(keyBytes, "AES")
            session.unlock(key)
            java.util.Arrays.fill(keyBytes, 0)
            unlockError.value = null
            verifying.value = false
        }
    }

    // --- List + note ---

    fun lockNow() {
        session.lock()
    }

    fun openNote(id: Long) {
        _phase.value = VaultPhase.Note(id)
    }

    fun backToList() {
        _phase.value = VaultPhase.List
        touch()
    }

    suspend fun loadNote(id: Long): LockedNote? {
        val key = session.key() ?: return null
        return repo.open(id, key)
    }

    suspend fun saveNote(note: LockedNote): Long? {
        val key = session.key() ?: return null
        touch()
        return repo.upsert(note, key)
    }

    suspend fun deleteNote(id: Long) {
        repo.delete(id)
    }

    fun deleteRow(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    class Factory(
        private val repo: VaultRepository,
        private val meta: VaultMetaStore,
        private val session: VaultSession,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            VaultViewModel(repo, meta, session) as T
    }
}
