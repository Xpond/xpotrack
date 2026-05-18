package com.xpotrack.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xpotrack.app.ui.theme.XpTokens
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Shared "WEEKDAY · MON D   HH:MM" strip sitting above the page title on
// Notes and Tasks. Recomposes once per wall-clock minute.
@Composable
fun DateTimeStrip() {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            val msToNextMinute = 60_000L - (System.currentTimeMillis() % 60_000L)
            delay(msToNextMinute)
            now = System.currentTimeMillis()
        }
    }
    val date = remember(now / 86_400_000L) {
        SimpleDateFormat("EEEE · MMM d", Locale.getDefault()).format(Date(now))
    }
    val time = remember(now / 60_000L) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(date.uppercase(), style = MaterialTheme.typography.labelSmall, color = XpTokens.Ink3)
        Spacer(Modifier.width(10.dp))
        Text(time, style = MaterialTheme.typography.labelSmall, color = XpTokens.Ink3)
    }
}
