package com.xpotrack.app.data.repo

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.xpotrack.app.data.db.NoteDao
import com.xpotrack.app.data.db.NoteEntity
import com.xpotrack.app.data.model.Category
import com.xpotrack.app.ui.notes.NoteRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class NotesRepository(
    private val dao: NoteDao,
    private val categories: CategoryRepository,
) {

    // Bounded search for the link-note picker. Empty `q` returns the most
    // recent `limit` notes; non-empty `q` filters by title LIKE. The DAO
    // applies the LIMIT in SQLite so we never materialize the full table.
    fun searchForPicker(q: String, limit: Int = 200): Flow<List<NoteRow>> =
        dao.searchForPicker(q, limit).combine(categories.observeAll()) { rows, cats ->
            val byId = cats.associateBy { it.id }
            rows.map { toUi(it, byId) }
        }

    // catFilter: -1 = no filter, 0 = uncategorized, >0 = that category id.
    // q: "" disables search.
    fun pagedNotes(catFilter: Long, q: String): Flow<PagingData<NoteRow>> {
        // Categories are snapshotted once per emitted PagingData. Renames/recolors
        // mid-scroll won't propagate to pages that have already loaded — the
        // alternative (joining the category flow) would recreate the Pager on
        // every category edit and lose scroll position. Acceptable trade-off.
        val pager = Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 30,
                initialLoadSize = 100,
                enablePlaceholders = true,
            ),
            pagingSourceFactory = { dao.pagingSource(catFilter, q) },
        )
        return pager.flow.map { data: PagingData<NoteEntity> ->
            val byId = categories.all().associateBy { it.id }
            data.map { entity -> toUi(entity, byId) }
        }
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

    // Counts for the filter bar menu. Streamed directly from SQLite so the
    // numbers stay fresh without ever loading the full note list.
    data class Counts(val total: Int, val uncategorized: Int, val byCategory: Map<Long, Int>)

    fun observeCounts(): Flow<Counts> =
        combine(
            dao.observeCount(),
            dao.observeUncategorizedCount(),
            dao.observePerCategoryCounts(),
        ) { total, uncat, perCat ->
            Counts(total = total, uncategorized = uncat, byCategory = perCat.associate { it.id to it.n })
        }

    suspend fun delete(id: Int) = dao.delete(id.toLong())

    // Category-only change — does NOT bump updatedAt. Reclassifying a note
    // shouldn't jump it to the top of the list.
    suspend fun setCategory(id: Int, categoryId: Long) =
        dao.setCategory(id.toLong(), categoryId.takeIf { it > 0L })

    private fun toUi(e: NoteEntity, byId: Map<Long, Category>): NoteRow {
        val cat = e.categoryId?.let { byId[it] }
        return NoteRow(
            id = e.id.toInt(),
            title = e.title,
            preview = e.bodyMarkdown,
            categoryId = cat?.id ?: 0L,
            categoryName = cat?.name ?: "Uncategorized",
            categoryColorHex = cat?.colorHex,
            when_ = formatWhen(e.updatedAt),
            updatedAt = e.updatedAt,
        )
    }
}
