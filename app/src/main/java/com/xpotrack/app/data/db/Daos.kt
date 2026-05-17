package com.xpotrack.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY recency DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<NoteEntity>)

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun count(): Int
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY time ASC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<TaskEntity>)

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun count(): Int
}

@Dao
interface MetaDao {
    @Query("SELECT value FROM meta WHERE `key` = :k LIMIT 1")
    suspend fun get(k: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(row: MetaEntity)
}
