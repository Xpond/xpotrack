package com.xpotrack.app.data.repo

import com.xpotrack.app.data.db.TaskDao
import com.xpotrack.app.data.db.TaskEntity
import com.xpotrack.app.data.model.ReminderLevel
import com.xpotrack.app.ui.tasks.TaskRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TasksRepository(private val dao: TaskDao) {

    fun observeAll(): Flow<List<TaskRow>> = dao.observeAll().map { rows -> rows.map(::toUi) }

    suspend fun seedIfEmpty(seed: List<TaskEntity>) {
        if (dao.count() == 0) dao.insertAll(seed)
    }

    private fun toUi(e: TaskEntity): TaskRow = TaskRow(
        id = e.id.toInt(),
        time = e.time,
        label = e.title,
        level = ReminderLevel.valueOf(e.level),
        durationMin = e.durationMin,
        done = e.isDone,
    )
}
