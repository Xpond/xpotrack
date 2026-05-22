package com.xpotrack.app

import android.app.Application
import com.xpotrack.app.data.alarm.AlarmScheduler
import com.xpotrack.app.data.alarm.NotificationChannels
import com.xpotrack.app.data.db.XpDatabase
import com.xpotrack.app.data.model.ReminderLevel
import com.xpotrack.app.data.prefs.EditorZoomPrefs
import com.xpotrack.app.data.prefs.ThemePrefs
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
    lateinit var themePrefs: ThemePrefs
        private set
    lateinit var editorZoomPrefs: EditorZoomPrefs
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        themePrefs = ThemePrefs(this).also { it.applyCurrent() }
        editorZoomPrefs = EditorZoomPrefs(this)
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

        // Preload notes off the main thread and gate the splash on the first
        // emission so the launcher icon hands off to a populated list.
        appScope.launch {
            db.noteDao().observeAll().first()
            SplashGate.notesReady = true
        }

        val seedJob = appScope.launch {
            val now = System.currentTimeMillis()
            notesRepo.seedIfEmpty(SeedData.notes(now))
            tasksRepo.seedIfEmpty(SeedData.tasks(now))
        }
        // Alarm rearm runs on its own coroutine so it can't queue behind seed
        // and starve the UI's first notes query. Waits for seed so freshly
        // seeded tasks get armed too.
        appScope.launch {
            seedJob.join()
            val now = System.currentTimeMillis()
            tasksRepo.observeAll().first().forEach { task ->
                if (task.level == ReminderLevel.Silent || task.isDone) return@forEach
                val next = alarmScheduler.nextOccurrence(task.dateEpochDay, task.time, now)
                if (task.reminderAt != next) tasksRepo.updateReminderAt(task.id, next)
                alarmScheduler.schedule(task.copy(reminderAt = next))
            }
        }
    }
}

// Splash screen keep-on-screen flag. Flipped once notes are queryable so
// MainActivity can hand off from the launcher icon to a populated list.
object SplashGate {
    @Volatile var notesReady: Boolean = false
}
