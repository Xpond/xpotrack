package com.xpotrack.app

import android.app.Application
import com.xpotrack.app.data.alarm.AlarmScheduler
import com.xpotrack.app.data.alarm.NotificationChannels
import com.xpotrack.app.data.backup.BackupManager
import com.xpotrack.app.data.db.XpDatabase
import com.xpotrack.app.data.model.ReminderLevel
import com.xpotrack.app.data.prefs.EditorZoomPrefs
import com.xpotrack.app.data.prefs.ThemePrefs
import com.xpotrack.app.data.quick.QuickNoteSweepWorker
import com.xpotrack.app.data.repo.CategoryRepository
import com.xpotrack.app.data.repo.NotesRepository
import com.xpotrack.app.data.repo.QuickNotesRepository
import com.xpotrack.app.data.repo.TasksRepository
import com.xpotrack.app.data.repo.VaultRepository
import com.xpotrack.app.data.security.VaultMetaStore
import com.xpotrack.app.data.security.VaultSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
    lateinit var backupManager: BackupManager
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
        backupManager = BackupManager(this)
        QuickNoteSweepWorker.enqueue(this)

        // Watchdog: NotesListScreen flips notesReady once the first paged page
        // settles. Release after 1.5s no matter what so a hung Paging refresh
        // can't leave the splash stuck forever.
        appScope.launch { delay(1500); SplashGate.notesReady = true }

        appScope.launch {
            tasksRepo.observeAll().first().forEach { task ->
                if (task.level == ReminderLevel.Silent || task.isDone) return@forEach
                // Re-upsert so overdue recurring tasks roll forward on the row
                // itself (not just the alarm), keeping date + reminderAt in sync.
                tasksRepo.upsert(task)
            }
        }
    }
}

// Splash screen keep-on-screen flag. Flipped by NotesListScreen once the
// first paged page settles, so MainActivity can hand off from the launcher
// icon to a populated list (not just a bare header). XpApp also arms a 1.5s
// watchdog. `taskReady` covers the notification cold-start path — splash
// holds until the deep-linked TaskDetailScreen has data, otherwise the
// first frame is an empty XpTokens.Bg box that reads as a black flash.
object SplashGate {
    @Volatile var notesReady: Boolean = false
    @Volatile var taskReady: Boolean = true
}
