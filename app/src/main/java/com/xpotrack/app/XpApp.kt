package com.xpotrack.app

import android.app.Application
import com.xpotrack.app.data.db.XpDatabase
import com.xpotrack.app.data.repo.NotesRepository
import com.xpotrack.app.data.repo.SeedData
import com.xpotrack.app.data.repo.TasksRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Tiny manual DI container — no Hilt, no Koin. We have two repos.
class XpApp : Application() {

    lateinit var notesRepo: NotesRepository
        private set
    lateinit var tasksRepo: TasksRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val db = XpDatabase.get(this)
        notesRepo = NotesRepository(db.noteDao())
        tasksRepo = TasksRepository(db.taskDao())

        appScope.launch {
            val now = System.currentTimeMillis()
            notesRepo.seedIfEmpty(SeedData.notes(now))
            tasksRepo.seedIfEmpty(SeedData.tasks(now))
        }
    }
}
