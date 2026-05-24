package com.xpotrack.app

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import com.xpotrack.app.ui.AppRoot
import com.xpotrack.app.ui.theme.XpTheme
import com.xpotrack.app.ui.theme.XpTokens
import kotlinx.coroutines.delay

class MainActivity : FragmentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result ignored */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { !SplashGate.notesReady }
        // Vivo/Funtouch (and other OEMs) ignore windowSplashScreenAnimatedIcon,
        // so on a restore-driven relaunch we paint our own logo splash inside
        // the activity instead of relying on the system splash.
        val restartPrefs = getSharedPreferences("xp_restart", Context.MODE_PRIVATE)
        val showRestartSplash = restartPrefs.getBoolean("restored_pending", false)
        if (showRestartSplash) restartPrefs.edit().remove("restored_pending").commit()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        hideStatusBarImmersive()
        maybeRequestNotificationPermission()
        setContent {
            XpTheme {
                Box(Modifier.fillMaxSize()) {
                    AppRoot()
                    if (showRestartSplash) RestartSplash()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        promptFullScreenIntentIfNeeded()
    }

    private fun hideStatusBarImmersive() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.statusBars())
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // Overlay splash for the restore-relaunch path. Stays opaque until notes
    // are ready AND a minimum visible duration has elapsed, then fades out.
    @Composable
    private fun RestartSplash() {
        val minVisibleUntil = remember { SystemClock.uptimeMillis() + 700 }
        var visible by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            while (!SplashGate.notesReady || SystemClock.uptimeMillis() < minVisibleUntil) {
                delay(50)
            }
            visible = false
        }
        AnimatedVisibility(visible = visible, exit = fadeOut(animationSpec = tween(220))) {
            Box(
                Modifier.fillMaxSize().background(XpTokens.Bg),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(160.dp),
                )
            }
        }
    }

    // Android 14+: USE_FULL_SCREEN_INTENT is a Special Access permission.
    // Without it, locked-screen alarms cannot take over. Re-prompt on every
    // resume until granted — the user just enables it once and is done.
    private fun promptFullScreenIntentIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.canUseFullScreenIntent()) return
        Toast.makeText(
            this,
            "Enable \"Full screen notifications\" for xpotrack so alarms work on the lock screen",
            Toast.LENGTH_LONG,
        ).show()
        val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
            .setData(Uri.parse("package:$packageName"))
        runCatching { startActivity(intent) }
    }
}

