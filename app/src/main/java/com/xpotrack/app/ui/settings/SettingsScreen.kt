package com.xpotrack.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.XpApp
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as XpApp
    var dark by remember { mutableStateOf(app.themePrefs.isDark) }
    val set: (Boolean) -> Unit = { v -> if (v != dark) { dark = v; app.themePrefs.isDark = v } }

    Column(
        modifier = modifier.fillMaxSize().background(XpTokens.Bg)
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text("PREFERENCES", style = MaterialTheme.typography.labelSmall, color = XpTokens.Ink3)
        Spacer(Modifier.height(8.dp))
        Text("Settings", style = MaterialTheme.typography.displayLarge, color = XpTokens.Ink)

        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Appearance", style = MaterialTheme.typography.labelSmall, color = XpTokens.Ink3)
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.clip(CircleShape).background(XpTokens.Surface2)
                    .border(0.5.dp, XpTokens.Hair, CircleShape).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Segment("Dark", dark) { set(true) }
                Segment("Light", !dark) { set(false) }
            }
        }
    }
}

@Composable
private fun Segment(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(CircleShape)
            .background(if (selected) XpTokens.Teal else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 14.dp),
    ) {
        Text(
            label,
            fontWeight = FontWeight.Medium, fontSize = 15.sp,
            color = if (selected) XpTokens.OnTeal else XpTokens.Ink3,
        )
    }
}
