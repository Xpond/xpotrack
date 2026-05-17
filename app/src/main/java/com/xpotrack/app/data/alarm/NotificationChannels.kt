package com.xpotrack.app.data.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager

// Two channels: quiet "Notify" reminders and high-importance "Alarm".
// Both set lockscreenVisibility = PUBLIC so the content shows on the lock
// screen, matching how stock clocks behave for alarms.
object NotificationChannels {

    // Bump the version suffix any time channel config changes — Android
    // ignores edits to an existing channel's importance / sound, so a fresh
    // ID is the only way to make them apply.
    const val NOTIFY_ID = "reminders.notify.v2"
    const val ALARM_ID = "reminders.alarm.v2"

    fun ensure(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(NOTIFY_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(NOTIFY_ID, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Notify-level task reminders"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
        }
        if (nm.getNotificationChannel(ALARM_ID) == null) {
            val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            nm.createNotificationChannel(
                NotificationChannel(ALARM_ID, "Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Full-screen alarms"
                    setSound(sound, attrs)
                    enableVibration(true)
                    setBypassDnd(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
        }
    }
}
