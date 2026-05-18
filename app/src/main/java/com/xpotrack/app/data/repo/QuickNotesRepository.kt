package com.xpotrack.app.data.repo

import androidx.room.withTransaction
import com.xpotrack.app.data.db.NoteEntity
import com.xpotrack.app.data.db.QuickNoteDao
import com.xpotrack.app.data.db.QuickNoteEntity
import com.xpotrack.app.data.db.XpDatabase
import kotlinx.coroutines.flow.Flow

class QuickNotesRepository(
    private val db: XpDatabase,
    private val dao: QuickNoteDao,
) {

    // The DAO's filter ("expiresAt > now") freezes `now` at subscribe time, so
    // expired rows would linger on screen until something else mutates the table.
    // The sweep on screen open + the periodic worker do the mutation — once
    // expired rows are deleted, Room re-emits.
    fun observe(): Flow<List<QuickNoteEntity>> = dao.observe(System.currentTimeMillis())

    suspend fun add(text: String): QuickNoteEntity {
        val now = System.currentTimeMillis()
        val row = QuickNoteEntity(
            text = text.trim(),
            createdAt = now,
            expiresAt = now + LIFETIME_MS,
        )
        val id = dao.upsert(row)
        return row.copy(id = id)
    }

    suspend fun getById(id: Long): QuickNoteEntity? = dao.getById(id)

    // Edits the text without resetting createdAt/expiresAt — a quick note still
    // disappears 24h after it was first written, no matter how often it's edited.
    suspend fun update(id: Long, text: String) {
        val existing = dao.getById(id) ?: return
        dao.upsert(existing.copy(text = text.trim()))
    }

    suspend fun sweepExpired(now: Long = System.currentTimeMillis()) {
        dao.deleteExpired(now)
    }

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun delete(id: Long) = dao.delete(id)

    // Move a quick note into the regular notes table as Uncategorized, deleting
    // the quick row in the same transaction so it can't end up in both lists.
    suspend fun keep(id: Long) {
        db.withTransaction {
            val q = dao.getById(id) ?: return@withTransaction
            val now = System.currentTimeMillis()
            val firstLine = q.text.lineSequence().firstOrNull()?.trim().orEmpty()
            val title = if (firstLine.length <= 60) firstLine else firstLine.take(57) + "…"
            db.noteDao().upsert(
                NoteEntity(
                    title = title.ifEmpty { "Quick note" },
                    bodyMarkdown = q.text,
                    categoryId = null,
                    createdAt = q.createdAt,
                    updatedAt = now,
                )
            )
            dao.delete(id)
        }
    }

    companion object {
        const val LIFETIME_MS = 24L * 60 * 60 * 1000
    }
}
