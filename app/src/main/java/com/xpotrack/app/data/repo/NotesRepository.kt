package com.xpotrack.app.data.repo

import com.xpotrack.app.data.db.NoteDao
import com.xpotrack.app.data.db.NoteEntity
import com.xpotrack.app.ui.notes.NoteRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotesRepository(private val dao: NoteDao) {

    fun observeAll(): Flow<List<NoteRow>> = dao.observeAll().map { rows -> rows.map(::toUi) }

    suspend fun seedIfEmpty(seed: List<NoteEntity>) {
        if (dao.count() == 0) dao.insertAll(seed)
    }

    private fun toUi(e: NoteEntity): NoteRow = NoteRow(
        id = e.id.toInt(),
        title = e.title,
        preview = e.bodyMarkdown,
        category = e.category,
        when_ = formatWhen(e.updatedAt),
        words = e.bodyMarkdown.split(Regex("\\s+")).filter { it.isNotBlank() }.size,
        recency = e.recency,
    )

    // Quick stub — real "Today / Yesterday / Tue / Apr 28" formatter lands later.
    private fun formatWhen(updatedAt: Long): String {
        val days = ((System.currentTimeMillis() - updatedAt) / 86_400_000L).toInt()
        return when {
            days <= 0 -> "Today"
            days == 1 -> "Yesterday"
            days < 7 -> "${days}d"
            else -> "${days}d ago"
        }
    }
}
