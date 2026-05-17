package com.xpotrack.app.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.xpotrack.app.data.model.ReminderLevel
import com.xpotrack.app.data.model.Task
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

// Wraps AlarmManager.setExactAndAllowWhileIdle for Notify/Alarm tasks.
// Silent tasks never schedule. Repository calls schedule() on upsert and
// cancel() before re-scheduling so the wall-clock change reflects in the OS.
class AlarmScheduler(private val context: Context) {

    private val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // Computes the next epoch-ms for an HH:mm in the device's local zone.
    // If today's time is already past, rolls to tomorrow.
    fun nextOccurrence(timeHHmm: String, now: Long = System.currentTimeMillis()): Long {
        val (h, m) = timeHHmm.split(":").map { it.toInt() }
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone).atTime(LocalTime.of(h, m)).atZone(zone)
        val todayMs = today.toInstant().toEpochMilli()
        return if (todayMs > now) todayMs else today.plusDays(1).toInstant().toEpochMilli()
    }

    fun schedule(task: Task) {
        cancel(task.id)
        if (task.level == ReminderLevel.Silent) return
        if (task.isDone) return
        if (task.reminderAt <= System.currentTimeMillis()) return

        val pi = pendingIntent(task)
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.reminderAt, pi)
    }

    fun cancel(taskId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_FIRE }
        val pi = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        am.cancel(pi)
        pi.cancel()
    }

    private fun pendingIntent(task: Task): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_LEVEL, task.level.name)
            putExtra(EXTRA_TITLE, task.title)
            putExtra(EXTRA_TIME, task.time)
        }
        return PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_FIRE = "com.xpotrack.app.ALARM_FIRE"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_LEVEL = "level"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TIME = "time"
    }
}
