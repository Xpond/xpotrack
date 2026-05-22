package com.xpotrack.app.ui.alarm

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.xpotrack.app.XpApp
import com.xpotrack.app.ui.theme.XpTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Full-screen takeover for ALARM-level reminders. Wakes the device, dismisses
// the keyguard (for non-secure locks; secure locks still require user unlock
// to interact, but the alarm UI is visible above the keyguard), plays the
// system alarm sound on a loop, vibrates.
class AlarmRingingActivity : ComponentActivity() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        showOverLockScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
        val time = intent.getStringExtra(EXTRA_TIME) ?: ""

        startRingingAndVibrating()

        val app = applicationContext as XpApp
        var noteSnippet by mutableStateOf<NoteSnippet?>(null)
        var repeatRule by mutableStateOf("none")

        lifecycleScope.launch {
            val (repeat, snippet) = withContext(Dispatchers.IO) {
                val t = app.tasksRepo.getById(taskId)
                val linked = t?.linkedNoteId?.toInt()?.let { app.notesRepo.getById(it) }
                val s = when {
                    linked != null -> NoteSnippet(linked.preview, linked.updatedAt, fromLinked = true)
                    !t?.notes.isNullOrBlank() -> NoteSnippet(t!!.notes, t.updatedAt, fromLinked = false)
                    else -> null
                }
                (t?.repeat ?: "none") to s
            }
            repeatRule = repeat
            noteSnippet = snippet
        }

        setContent {
            XpTheme {
                AlarmRingingScreen(
                    title = title,
                    time = time,
                    repeat = repeatRule,
                    note = noteSnippet,
                    onSnooze = { minutes ->
                        app.alarmScheduler.snooze(taskId, title, time, minutes)
                        cancelNotification(taskId)
                        finish()
                    },
                    onDone = {
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) { app.tasksRepo.markDone(taskId) }
                            cancelNotification(taskId)
                            finish()
                        }
                    },
                )
            }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            )
        }
    }

    private fun startRingingAndVibrating() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            setDataSource(this@AlarmRingingActivity, uri)
            isLooping = true
            prepare()
            start()
        }
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 500, 700)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun cancelNotification(taskId: Long) {
        if (taskId < 0) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(taskId.toInt())
    }

    override fun onDestroy() {
        player?.runCatching { stop(); release() }
        player = null
        vibrator?.cancel()
        vibrator = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TIME = "time"
    }
}
