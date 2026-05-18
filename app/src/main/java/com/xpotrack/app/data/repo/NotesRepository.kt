package com.xpotrack.app.data.repo

import com.xpotrack.app.data.db.NoteDao
import com.xpotrack.app.data.db.NoteEntity
import com.xpotrack.app.data.model.Category
import com.xpotrack.app.ui.notes.NoteRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class NotesRepository(
    private val dao: NoteDao,
    private val categories: CategoryRepository,
) {

    // Joins the live note stream against the live category stream so renames /
    // recolors propagate without manual cache invalidation.
    fun observeAll(): Flow<List<NoteRow>> =
        dao.observeAll().combine(categories.observeAll()) { rows, cats ->
            val byId = cats.associateBy { it.id }
            rows.map { toUi(it, byId) }
        }

    suspend fun getById(id: Int): NoteRow? {
        val e = dao.getById(id.toLong()) ?: return null
        val byId = categories.all().associateBy { it.id }
        return toUi(e, byId)
    }

    suspend fun upsert(row: NoteRow): Long {
        val now = System.currentTimeMillis()
        val existing = if (row.id > 0) dao.getById(row.id.toLong()) else null
        val entity = NoteEntity(
            id = row.id.toLong(),
            title = row.title,
            bodyMarkdown = row.preview,
            categoryId = row.categoryId.takeIf { it > 0L },
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

    private fun toUi(e: NoteEntity, byId: Map<Long, Category>): NoteRow {
        val cat = e.categoryId?.let { byId[it] }
        return NoteRow(
            id = e.id.toInt(),
            title = e.title,
            preview = e.bodyMarkdown,
            categoryId = cat?.id ?: 0L,
            categoryName = cat?.name ?: "Uncategorized",
            when_ = formatWhen(e.updatedAt),
        )
    }
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
