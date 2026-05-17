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

// Android drops exact alarms across reboots; re-arm everything that's still
// in the future for non-Silent tasks. Recomputes reminderAt so a task whose
// HH:mm has already passed today rolls to tomorrow.
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
                    val next = app.alarmScheduler.nextOccurrence(task.time)
                    app.tasksRepo.updateReminderAt(task.id, next)
                    app.alarmScheduler.schedule(task.copy(reminderAt = next))
                }
            } finally {
                pending.finish()
            }
        }
    }
}
