package com.xpotrack.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.xpotrack.app.R
import com.xpotrack.app.data.model.ReminderLevel
import com.xpotrack.app.ui.theme.XpTokens

@Immutable
data class ReminderStyle(
    val accent: Color,
    val tint: Color,
    val cardBg: Color,
    val iconRes: Int,
)

@Composable
fun styleFor(level: ReminderLevel): ReminderStyle = when (level) {
    ReminderLevel.Silent -> ReminderStyle(
        XpTokens.Silent, XpTokens.Silent,
        Color(0x0A6B807D), R.drawable.ic_reminder_silent,
    )
    ReminderLevel.Notify -> ReminderStyle(
        XpTokens.Notify, XpTokens.Notify,
        Color(0x0D5EEAD4), R.drawable.ic_reminder_notify,
    )
    ReminderLevel.Alarm -> ReminderStyle(
        XpTokens.Alarm, XpTokens.Alarm,
        Color(0x0FFBBF24), R.drawable.ic_reminder_alarm,
    )
}
