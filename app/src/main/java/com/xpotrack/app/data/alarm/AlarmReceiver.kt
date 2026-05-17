package com.xpotrack.app.data.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.xpotrack.app.MainActivity
import com.xpotrack.app.R
import com.xpotrack.app.data.model.ReminderLevel
import com.xpotrack.app.ui.alarm.AlarmRingingActivity

private const val TEAL_ARGB = 0xFF5EEAD4.toInt()

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
        when (level) {
            ReminderLevel.Alarm -> launchRinging(context, taskId, title, time)
            ReminderLevel.Notify -> postNotification(context, taskId, title, time)
            ReminderLevel.Silent -> Unit
        }
    }

    private fun postNotification(context: Context, taskId: Long, title: String, time: String) {
        val tap = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
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
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(taskId.toInt(), n)
    }

    private fun launchRinging(context: Context, taskId: Long, title: String, time: String) {
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
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
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
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(taskId.toInt(), n)
    }
}
