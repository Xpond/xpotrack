package com.xpotrack.app.ui.quick

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xpotrack.app.data.repo.QuickNotesRepository
import kotlinx.coroutines.launch

// Only the editor talks to this VM now — the index reads from NotesViewModel's
// merged feed. Keeping a tiny VM just so QuickEditorScreen can stay unchanged.
class QuickNotesViewModel(
    private val repo: QuickNotesRepository,
) : ViewModel() {

    fun add(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        viewModelScope.launch { repo.add(t) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    suspend fun getText(id: Long): String = repo.getById(id)?.text.orEmpty()

    fun update(id: Long, text: String) {
        viewModelScope.launch { repo.update(id, text) }
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
    val leftLabel: String,
    val pct: Int,
    val expiring: Boolean,
)

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
