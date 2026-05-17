package com.xpotrack.app.data.repo

import com.xpotrack.app.data.db.CategoryDao
import com.xpotrack.app.data.db.CategoryEntity
import com.xpotrack.app.data.db.NoteDao
import com.xpotrack.app.data.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// All category lifecycle lives here. Notes screens read the same flow so they
// can resolve categoryId → name + color when mapping to UI rows.
class CategoryRepository(
    private val dao: CategoryDao,
    private val noteDao: NoteDao,
) {

    fun observeAll(): Flow<List<Category>> = dao.observeAll().map { rows -> rows.map(::toDomain) }

    suspend fun all(): List<Category> = dao.all().map(::toDomain)

    suspend fun getById(id: Long): Category? = dao.getById(id)?.let(::toDomain)

    suspend fun add(name: String, colorHex: String): Long {
        val sortOrder = dao.count()
        return dao.upsert(CategoryEntity(
            name = name.trim(), colorHex = colorHex, sortOrder = sortOrder,
        ))
    }

    suspend fun rename(id: Long, name: String) = dao.rename(id, name.trim())
    suspend fun recolor(id: Long, colorHex: String) = dao.recolor(id, colorHex)

    // Delete a category; notes pointing at it become Uncategorized (categoryId = null).
    suspend fun deleteAndUncategorize(id: Long) {
        dao.deleteAndUncategorize(id, noteDao)
    }

    suspend fun noteUsage(id: Long): Int = noteDao.countInCategory(id)

    private fun toDomain(e: CategoryEntity): Category = Category(
        id = e.id, name = e.name, colorHex = e.colorHex, sortOrder = e.sortOrder,
    )
}
