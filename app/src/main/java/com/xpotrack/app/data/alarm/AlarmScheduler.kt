package com.xpotrack.app.data.alarm

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.xpotrack.app.MainActivity
import com.xpotrack.app.data.model.ReminderLevel
import com.xpotrack.app.data.model.Task
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

// Schedules wall-clock reminders via AlarmManager. Notify uses
// setExactAndAllowWhileIdle; Alarm uses setAlarmClock so it survives
// app-standby buckets. Silent never schedules. Repository calls schedule() on
// upsert (which cancels first) so wall-clock edits reflect in the OS.
class AlarmScheduler(private val context: Context) {

    private val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // Resolves a task's (dateEpochDay, HH:mm) into an absolute epoch-ms.
    // Returns 0 if the moment is in the past — schedule() then no-ops, so
    // overdue tasks never re-arm an alarm. dateEpochDay == 0L means "no date
    // set" (pre-migration default) and is also treated as unscheduled.
    fun nextOccurrence(
        dateEpochDay: Long,
        timeHHmm: String,
        now: Long = System.currentTimeMillis(),
    ): Long {
        if (dateEpochDay <= 0L) return 0L
        val (h, m) = timeHHmm.split(":").map { it.toInt() }
        val zone = ZoneId.systemDefault()
        val ms = LocalDate.ofEpochDay(dateEpochDay)
            .atTime(LocalTime.of(h, m))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        return if (ms > now) ms else 0L
    }

    fun schedule(task: Task) {
        cancel(task.id)
        if (task.level == ReminderLevel.Silent) return
        if (task.isDone) return
        if (task.reminderAt <= System.currentTimeMillis()) return

        val pi = pendingIntent(task)
        if (task.level == ReminderLevel.Alarm) {
            // setAlarmClock is the only API that bypasses app-standby buckets,
            // so Alarm-level reminders fire on the wall clock even when the
            // device has demoted the app. Side effect: status-bar alarm icon.
            am.setAlarmClock(AlarmClockInfo(task.reminderAt, showIntent()), pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.reminderAt, pi)
        }
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

    // One-shot snooze. Lives in a separate request-code namespace from the
    // recurring schedule() PendingIntent, so snoozing a recurring task doesn't
    // overwrite tomorrow's already-armed alarm.
    fun snooze(taskId: Long, title: String, timeHHmm: String, minutes: Int) {
        val at = System.currentTimeMillis() + minutes * 60_000L
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_LEVEL, ReminderLevel.Alarm.name)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_TIME, timeHHmm)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            taskId.toInt() xor SNOOZE_SALT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.setAlarmClock(AlarmClockInfo(at, showIntent()), pi)
    }

    private fun showIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

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
        private const val SNOOZE_SALT = 0x5E0_0_2E
    }
}
