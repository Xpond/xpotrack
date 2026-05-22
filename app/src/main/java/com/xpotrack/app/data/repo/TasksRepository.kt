package com.xpotrack.app.data.repo

import com.xpotrack.app.data.alarm.AlarmScheduler
import com.xpotrack.app.data.db.TaskDao
import com.xpotrack.app.data.db.TaskEntity
import com.xpotrack.app.data.model.ReminderLevel
import com.xpotrack.app.data.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
            else scheduler.nextOccurrence(task.dateEpochDay, task.time, task.repeat, now)
        // Mirror the rolled-forward date onto the row so the UI shows the
        // next valid occurrence instead of the missed one.
        val effectiveDate = if (reminderAt > 0L)
            Instant.ofEpochMilli(reminderAt).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
            else task.dateEpochDay
        val entity = TaskEntity(
            id = task.id,
            title = task.title,
            time = task.time,
            level = task.level.name,
            durationMin = task.durationMin,
            notes = task.notes,
            isDone = task.isDone,
            reminderAt = reminderAt,
            dateEpochDay = effectiveDate,
            repeat = task.repeat,
            linkedNoteId = task.linkedNoteId,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        val newId = dao.upsert(entity)
        scheduler.schedule(toDomain(entity).copy(id = if (task.id == 0L) newId else task.id))
        return newId
    }

    suspend fun markDone(id: Long) {
        val existing = dao.getById(id) ?: return
        if (existing.repeat != "none") {
            // Repeating tasks never "complete" — they roll forward to the next
            // occurrence and the alarm re-arms in upsert().
            val next = nextDateFor(existing.repeat, existing.dateEpochDay)
            upsert(toDomain(existing).copy(dateEpochDay = next))
            return
        }
        dao.markDone(id, System.currentTimeMillis())
        scheduler.cancel(id)
    }

    suspend fun delete(id: Long) {
        scheduler.cancel(id)
        dao.delete(id)
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
        repeat = e.repeat,
        linkedNoteId = e.linkedNoteId,
        createdAt = e.createdAt,
        updatedAt = e.updatedAt,
    )
}

// Next occurrence date for a recurring task, given the previous date.
// "none" returns the same day (caller treats it as "no roll-forward").
// "daily" → +1 day. "weekly" → +7. "weekdays" → next Mon..Fri (skip Sat/Sun).
fun nextDateFor(rule: String, fromEpochDay: Long): Long {
    val d = LocalDate.ofEpochDay(fromEpochDay)
    return when (rule) {
        "daily" -> d.plusDays(1).toEpochDay()
        "weekly" -> d.plusDays(7).toEpochDay()
        "weekdays" -> {
            var n = d.plusDays(1)
            while (n.dayOfWeek == DayOfWeek.SATURDAY || n.dayOfWeek == DayOfWeek.SUNDAY) {
                n = n.plusDays(1)
            }
            n.toEpochDay()
        }
        else -> fromEpochDay
    }
}
