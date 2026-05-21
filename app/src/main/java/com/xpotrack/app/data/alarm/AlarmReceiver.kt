package com.xpotrack.app.data.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.xpotrack.app.MainActivity
import com.xpotrack.app.R
import com.xpotrack.app.XpApp
import com.xpotrack.app.data.model.ReminderLevel
import com.xpotrack.app.data.repo.nextDateFor
import com.xpotrack.app.ui.alarm.AlarmRingingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TEAL_ARGB = 0xFF5EEAD4.toInt()
private const val PI_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

// Fires when AlarmManager dispatches our exact alarm. Routes by level:
// Notify -> heads-up notification; Alarm -> full-screen activity.
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_FIRE) return
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "Reminder"
        val time = intent.getStringExtra(AlarmScheduler.EXTRA_TIME) ?: ""
        val levelName = intent.getStringExtra(AlarmScheduler.EXTRA_LEVEL) ?: ReminderLevel.Notify.name
        val level = runCatching { ReminderLevel.valueOf(levelName) }.getOrDefault(ReminderLevel.Notify)

        NotificationChannels.ensure(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        when (level) {
            ReminderLevel.Alarm -> launchRinging(context, nm, taskId, title, time)
            ReminderLevel.Notify -> postNotification(context, nm, taskId, title, time)
            ReminderLevel.Silent -> Unit
        }
        rollForwardIfRecurring(context, taskId)
    }

    // Repeating tasks auto-advance to the next occurrence so the alarm
    // re-arms. Non-recurring tasks fall through untouched.
    private fun rollForwardIfRecurring(context: Context, taskId: Long) {
        if (taskId <= 0L) return
        val app = context.applicationContext as? XpApp ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = app.tasksRepo.getById(taskId) ?: return@launch
                if (task.repeat == "none") return@launch
                val next = nextDateFor(task.repeat, task.dateEpochDay)
                app.tasksRepo.upsert(task.copy(dateEpochDay = next, isDone = false))
            } finally {
                pending.finish()
            }
        }
    }

    private fun postNotification(context: Context, nm: NotificationManager, taskId: Long, title: String, time: String) {
        val tap = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PI_FLAGS,
        )
        val n = NotificationCompat.Builder(context, NotificationChannels.NOTIFY_ID)
            .setSmallIcon(R.drawable.ic_reminder_notify)
            .setContentTitle(title)
            .setContentText(if (time.isNotEmpty()) "Reminder · $time" else "Reminder")
            .setContentIntent(tap)
            .setAutoCancel(true)
            .setColor(TEAL_ARGB)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        nm.notify(taskId.toInt(), n)
    }

    private fun launchRinging(context: Context, nm: NotificationManager, taskId: Long, title: String, time: String) {
        val full = Intent(context, AlarmRingingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(AlarmRingingActivity.EXTRA_TASK_ID, taskId)
            putExtra(AlarmRingingActivity.EXTRA_TITLE, title)
            putExtra(AlarmRingingActivity.EXTRA_TIME, time)
        }
        val fullPi = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            full,
            PI_FLAGS,
        )
        // setFullScreenIntent is the *only* supported path for waking a locked
        // device from a background BroadcastReceiver. The notification system
        // handles bypassing the background-activity-start restriction; calling
        // context.startActivity() ourselves was silently dropped on lock.
        val n = NotificationCompat.Builder(context, NotificationChannels.ALARM_ID)
            .setSmallIcon(R.drawable.ic_reminder_alarm)
            .setContentTitle(title)
            .setContentText(if (time.isNotEmpty()) "Alarm · $time" else "Alarm")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(TEAL_ARGB)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullPi, true)
            .setOngoing(true)
            .setAutoCancel(true)
            .build()
        nm.notify(taskId.toInt(), n)
    }
}
