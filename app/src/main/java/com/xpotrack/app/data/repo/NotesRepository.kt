package com.xpotrack.app.data.repo

import com.xpotrack.app.data.db.NoteDao
import com.xpotrack.app.data.db.NoteEntity
import com.xpotrack.app.ui.notes.NoteRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class NotesRepository(private val dao: NoteDao) {

    fun observeAll(): Flow<List<NoteRow>> = dao.observeAll().map { rows -> rows.map(::toUi) }

    suspend fun getById(id: Int): NoteRow? = dao.getById(id.toLong())?.let(::toUi)

    suspend fun upsert(row: NoteRow): Long {
        val now = System.currentTimeMillis()
        val existing = if (row.id > 0) dao.getById(row.id.toLong()) else null
        val entity = NoteEntity(
            id = row.id.toLong(),
            title = row.title,
            bodyMarkdown = row.preview,
            category = row.category,
            isPinned = row.isPinned,
            isLocked = existing?.isLocked ?: false,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        return dao.upsert(entity)
    }

    suspend fun delete(id: Int) = dao.delete(id.toLong())

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
        isPinned = e.isPinned,
    )
}

private val zone: ZoneId = ZoneId.systemDefault()
private val monthDay = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
private val weekday = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)

internal fun formatWhen(updatedAt: Long, today: LocalDate = LocalDate.now(zone)): String {
    val d = Instant.ofEpochMilli(updatedAt).atZone(zone).toLocalDate()
    return when (val days = ChronoUnit.DAYS.between(d, today).toInt()) {
        0 -> "Today"
        1 -> "Yesterday"
        in 2..6 -> d.format(weekday)
        else -> d.format(monthDay)
    }
}
