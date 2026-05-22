package com.xpotrack.app.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xpotrack.app.XpApp
import com.xpotrack.app.data.model.ReminderLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Android drops exact alarms across reboots; re-upsert every active task so
// the scheduler re-arms it. Overdue recurring tasks get their date rolled
// forward to the next valid occurrence as part of the upsert path.
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as XpApp
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = app.tasksRepo.observeAll().first()
                tasks.forEach { task ->
                    if (task.level == ReminderLevel.Silent || task.isDone) return@forEach
                    // Re-upsert so overdue recurring tasks roll forward in the
                    // row itself, not just in the alarm — keeps row date and
                    // alarm in sync without duplicating the roll-forward math.
                    app.tasksRepo.upsert(task)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
