package com.xpotrack.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R
import com.xpotrack.app.XpApp
import com.xpotrack.app.ui.components.cutoutSafeTopPadding
import com.xpotrack.app.ui.theme.XpTokens

private val SettingsPillRowWidth = 264.dp

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as XpApp
    var dark by remember { mutableStateOf(app.themePrefs.isDark) }
    val set: (Boolean) -> Unit = { v -> if (v != dark) { dark = v; app.themePrefs.isDark = v } }

    Column(
        modifier = modifier.fillMaxSize().background(XpTokens.Bg)
            .cutoutSafeTopPadding()
            .padding(horizontal = 22.dp),
    ) {
        Text("PREFERENCES", style = MaterialTheme.typography.labelSmall, color = XpTokens.Ink3)
        Spacer(Modifier.height(8.dp))
        Text("Settings", style = MaterialTheme.typography.displayLarge, color = XpTokens.Ink)

        Column(
            Modifier.fillMaxWidth().weight(1f).padding(bottom = 90.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SettingsRow(modifier = Modifier.width(SettingsPillRowWidth)) {
                Segment(R.drawable.ic_moon, "Dark", dark, Modifier.weight(1f)) { set(true) }
                Segment(R.drawable.ic_sun, "Light", !dark, Modifier.weight(1f)) { set(false) }
            }
            Spacer(Modifier.height(18.dp))
            BackupSection(pillWidth = SettingsPillRowWidth)
        }
    }
}

@Composable
internal fun Modifier.tealPillOutline(): Modifier {
    val stroke = with(LocalDensity.current) { 0.5.dp.toPx() }
    return drawBehind {
        val inset = stroke / 2f
        drawRoundRect(
            color = XpTokens.Teal,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(size.height / 2f),
            style = Stroke(width = stroke),
        )
    }
}

@Composable
private fun SettingsRow(
    modifier: Modifier = Modifier,
    pills: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier.clip(CircleShape).background(XpTokens.Surface2)
            .tealPillOutline().padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { pills() }
}

@Composable
internal fun PillContent(iconRes: Int, label: String, tint: Color) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(16.dp),
    )
    Spacer(Modifier.width(8.dp))
    Text(
        label,
        fontWeight = FontWeight.Medium, fontSize = 15.sp,
        color = tint,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Segment(iconRes: Int, label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(CircleShape)
            .background(if (selected) XpTokens.Teal else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PillContent(iconRes, label, if (selected) XpTokens.OnTeal else XpTokens.Ink3)
        }
    }
}
