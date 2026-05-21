package com.xpotrack.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isLocked = 0 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isLocked = 1 ORDER BY updatedAt DESC")
    fun observeLocked(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<NoteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE notes SET categoryId = NULL WHERE categoryId = :fromId")
    suspend fun clearCategory(fromId: Long)

    @Query("SELECT COUNT(*) FROM notes WHERE isLocked = 0")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM notes WHERE categoryId = :id AND isLocked = 0")
    suspend fun countInCategory(id: Long): Int
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY time ASC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: TaskEntity): Long

    @Query("UPDATE tasks SET reminderAt = :at WHERE id = :id")
    suspend fun setReminderAt(id: Long, at: Long)

    @Query("UPDATE tasks SET isDone = 1, updatedAt = :now WHERE id = :id")
    suspend fun markDone(id: Long, now: Long)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun count(): Int
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<CategoryEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: CategoryEntity): Long

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE categories SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE categories SET colorHex = :colorHex WHERE id = :id")
    suspend fun recolor(id: Long, colorHex: String)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Transaction
    suspend fun deleteAndUncategorize(id: Long, noteDao: NoteDao) {
        noteDao.clearCategory(id)
        delete(id)
    }
}

@Dao
interface QuickNoteDao {
    @Query("SELECT * FROM quick_notes WHERE expiresAt > :now ORDER BY createdAt DESC")
    fun observe(now: Long): Flow<List<QuickNoteEntity>>

    @Query("SELECT * FROM quick_notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): QuickNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: QuickNoteEntity): Long

    @Query("DELETE FROM quick_notes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM quick_notes WHERE expiresAt <= :now")
    suspend fun deleteExpired(now: Long)

    @Query("DELETE FROM quick_notes")
    suspend fun deleteAll()
}

@Dao
interface MetaDao {
    @Query("SELECT value FROM meta WHERE `key` = :k LIMIT 1")
    suspend fun get(k: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(row: MetaEntity)
}
