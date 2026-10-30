package com.xpotrack.app.data.repo

import com.xpotrack.app.data.db.ExpenseDao
import com.xpotrack.app.data.db.ExpenseDayEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ExpenseRepository(private val dao: ExpenseDao, private val scope: CoroutineScope) {

    // Every keystroke saves. Mutex is FIFO, so writes land in typing order and
    // a reload of a day waits for that day's pending writes.
    private val lock = Mutex()

    fun observeRange(from: Long, to: Long): Flow<List<ExpenseDayEntity>> = dao.observeRange(from, to)

    fun observeDates(): Flow<List<Long>> = dao.observeDates()

    suspend fun get(day: Long): String = lock.withLock { dao.getText(day).orEmpty() }

    // App-scoped so leaving the app can't cancel queued writes. UNDISPATCHED takes
    // the lock's queue slot on the caller's thread, which keeps typing order.
    // A day cleared to blank drops its row so it leaves the month list.
    fun save(day: Long, text: String) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            lock.withLock {
                if (text.isBlank()) dao.delete(day) else dao.upsert(ExpenseDayEntity(day, text))
            }
        }
    }
}
