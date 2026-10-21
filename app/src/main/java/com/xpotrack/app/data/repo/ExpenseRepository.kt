package com.xpotrack.app.data.repo

import com.xpotrack.app.data.db.ExpenseDao
import com.xpotrack.app.data.db.ExpenseDayEntity
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val dao: ExpenseDao) {

    fun observeRange(from: Long, to: Long): Flow<List<ExpenseDayEntity>> = dao.observeRange(from, to)

    fun observeDates(): Flow<List<Long>> = dao.observeDates()

    suspend fun get(day: Long): String = dao.getText(day).orEmpty()

    // A day cleared to blank drops its row so it leaves the month list.
    suspend fun save(day: Long, text: String) {
        if (text.isBlank()) dao.delete(day) else dao.upsert(ExpenseDayEntity(day, text))
    }
}
