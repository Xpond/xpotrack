package com.xpotrack.app.data.repo

import com.xpotrack.app.data.alarm.AlarmScheduler
import com.xpotrack.app.data.db.TaskDao
import com.xpotrack.app.data.db.TaskEntity
import com.xpotrack.app.data.model.ReminderLevel
import com.xpotrack.app.data.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TasksRepository(
    private val dao: TaskDao,
    private val scheduler: AlarmScheduler,
) {

    fun observeAll(): Flow<List<Task>> = dao.observeAll().map { rows -> rows.map(::toDomain) }

    suspend fun getById(id: Long): Task? = dao.getById(id)?.let(::toDomain)

    suspend fun upsert(task: Task): Long {
        val now = System.currentTimeMillis()
        val existing = if (task.id != 0L) dao.getById(task.id) else null
        // Recompute reminderAt from the wall-clock HH:mm every write so edits
        // to the time wheel always re-target the next occurrence. Silent tasks
        // keep reminderAt = 0L; the scheduler then becomes a no-op.
        val reminderAt = if (task.level == ReminderLevel.Silent) 0L
            else scheduler.nextOccurrence(task.dateEpochDay, task.time, now)
        val entity = TaskEntity(
            id = task.id,
            title = task.title,
            time = task.time,
            level = task.level.name,
            durationMin = task.durationMin,
            notes = task.notes,
            isDone = task.isDone,
            reminderAt = reminderAt,
            dateEpochDay = task.dateEpochDay,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        val newId = dao.upsert(entity)
        scheduler.schedule(toDomain(entity).copy(id = if (task.id == 0L) newId else task.id))
        return newId
    }

    suspend fun updateReminderAt(id: Long, at: Long) = dao.setReminderAt(id, at)

    suspend fun markDone(id: Long) {
        dao.markDone(id, System.currentTimeMillis())
        scheduler.cancel(id)
    }

    suspend fun delete(id: Long) {
        scheduler.cancel(id)
        dao.delete(id)
    }

    suspend fun seedIfEmpty(seed: List<TaskEntity>) {
        if (dao.count() == 0) dao.insertAll(seed)
    }

    private fun toDomain(e: TaskEntity): Task = Task(
        id = e.id,
        title = e.title,
        time = e.time,
        level = ReminderLevel.valueOf(e.level),
        durationMin = e.durationMin,
        notes = e.notes,
        isDone = e.isDone,
        reminderAt = e.reminderAt,
        dateEpochDay = e.dateEpochDay,
        createdAt = e.createdAt,
        updatedAt = e.updatedAt,
    )
}
