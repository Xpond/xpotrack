package com.xpotrack.app.ui.quick

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xpotrack.app.data.db.QuickNoteEntity
import com.xpotrack.app.data.repo.QuickNotesRepository
import com.xpotrack.app.data.repo.QuickNotesRepository.Companion.LIFETIME_MS
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuickNotesViewModel(
    private val repo: QuickNotesRepository,
) : ViewModel() {

    // Ticks every 30s — countdown chips need a periodic recompute. Cheap, only
    // active while Quick screen is on screen (collectAsStateWithLifecycle).
    private val tick = MutableStateFlow(System.currentTimeMillis())

    val rows: StateFlow<List<QuickRow>> =
        combine(repo.observe(), tick) { entities, now -> entities.map { it.toRow(now) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _justSaved = MutableStateFlow<SavedDialog?>(null)
    val justSaved: StateFlow<SavedDialog?> = _justSaved.asStateFlow()

    init {
        // Sweep on open — the on-screen source of truth. WorkManager handles the
        // case where the user never opens this surface.
        viewModelScope.launch { repo.sweepExpired() }
        viewModelScope.launch {
            while (true) { delay(30_000); tick.value = System.currentTimeMillis() }
        }
    }

    fun add(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        viewModelScope.launch {
            val row = repo.add(t)
            _justSaved.value = SavedDialog(id = row.id, expiresAt = row.expiresAt)
        }
    }

    fun dismissDialog() { _justSaved.value = null }

    fun keepThenDismiss() {
        val d = _justSaved.value ?: return
        viewModelScope.launch {
            repo.keep(d.id)
            _justSaved.value = null
        }
    }

    fun keep(id: Long) {
        viewModelScope.launch { repo.keep(id) }
    }

    fun clearAll() {
        viewModelScope.launch { repo.deleteAll() }
    }

    class Factory(private val repo: QuickNotesRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuickNotesViewModel(repo) as T
    }
}

data class QuickRow(
    val id: Long,
    val text: String,
    val ageLabel: String,   // "just now", "2h ago"
    val leftLabel: String,  // "23h 51m", "2h 11m"
    val pct: Int,           // 0..100 of lifetime remaining
    val expiring: Boolean,  // pct < 15
)

data class SavedDialog(val id: Long, val expiresAt: Long)

private fun QuickNoteEntity.toRow(now: Long): QuickRow {
    val remaining = (expiresAt - now).coerceAtLeast(0)
    val ageMs = (now - createdAt).coerceAtLeast(0)
    val pct = ((remaining.toDouble() / LIFETIME_MS) * 100).toInt().coerceIn(0, 100)
    return QuickRow(
        id = id,
        text = text,
        ageLabel = ageLabel(ageMs),
        leftLabel = remainingLabel(remaining),
        pct = pct,
        expiring = pct < 15,
    )
}

private fun ageLabel(ms: Long): String {
    val mins = ms / 60_000
    if (mins < 1) return "just now"
    if (mins < 60) return "${mins}m ago"
    val hours = mins / 60
    return "${hours}h ago"
}

internal fun remainingLabel(ms: Long): String {
    if (ms <= 0) return "expired"
    val totalSecs = (ms / 1000).toInt()
    val h = totalSecs / 3600
    val m = (totalSecs % 3600) / 60
    val s = totalSecs % 60
    return when {
        h > 0 -> "${h}h ${m.toString().padStart(2, '0')}m"
        m > 0 -> "${m}m ${s.toString().padStart(2, '0')}s"
        else -> "${s}s"
    }
}
