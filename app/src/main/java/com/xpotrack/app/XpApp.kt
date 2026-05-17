package com.xpotrack.app

import android.app.Application
import com.xpotrack.app.data.alarm.AlarmScheduler
import com.xpotrack.app.data.alarm.NotificationChannels
import com.xpotrack.app.data.db.XpDatabase
import com.xpotrack.app.data.model.ReminderLevel
import com.xpotrack.app.data.quick.QuickNoteSweepWorker
import com.xpotrack.app.data.repo.CategoryRepository
import com.xpotrack.app.data.repo.NotesRepository
import com.xpotrack.app.data.repo.QuickNotesRepository
import com.xpotrack.app.data.repo.SeedData
import com.xpotrack.app.data.repo.TasksRepository
import com.xpotrack.app.data.repo.VaultRepository
import com.xpotrack.app.data.security.VaultMetaStore
import com.xpotrack.app.data.security.VaultSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Tiny manual DI container — no Hilt, no Koin.
class XpApp : Application() {

    lateinit var notesRepo: NotesRepository
        private set
    lateinit var tasksRepo: TasksRepository
        private set
    lateinit var categoryRepo: CategoryRepository
        private set
    lateinit var alarmScheduler: AlarmScheduler
        private set
    lateinit var vaultRepo: VaultRepository
        private set
    lateinit var vaultMeta: VaultMetaStore
        private set
    lateinit var vaultSession: VaultSession
        private set
    lateinit var quickNotesRepo: QuickNotesRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensure(this)
        val db = XpDatabase.get(this)
        alarmScheduler = AlarmScheduler(this)
        categoryRepo = CategoryRepository(db.categoryDao(), db.noteDao())
        notesRepo = NotesRepository(db.noteDao(), categoryRepo)
        tasksRepo = TasksRepository(db.taskDao(), alarmScheduler)
        vaultRepo = VaultRepository(db.noteDao())
        vaultMeta = VaultMetaStore(this)
        vaultSession = VaultSession()
        quickNotesRepo = QuickNotesRepository(db, db.quickNoteDao())
        QuickNoteSweepWorker.enqueue(this)

        appScope.launch {
            val now = System.currentTimeMillis()
            notesRepo.seedIfEmpty(SeedData.notes(now))
            tasksRepo.seedIfEmpty(SeedData.tasks(now))
            // Seed inserts go straight via the DAO, so they bypass the
            // schedule-on-upsert path. Walk the table once and arm anything
            // that's Notify/Alarm and not already done.
            tasksRepo.observeAll().first().forEach { task ->
                if (task.level == ReminderLevel.Silent || task.isDone) return@forEach
                val next = alarmScheduler.nextOccurrence(task.time, now)
                if (task.reminderAt != next) tasksRepo.updateReminderAt(task.id, next)
                alarmScheduler.schedule(task.copy(reminderAt = next))
            }
        }
    }
}
