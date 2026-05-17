package com.xpotrack.app.ui.quick

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xpotrack.app.R
import com.xpotrack.app.ui.theme.XpTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun QuickSavedDialog(d: SavedDialog, onGotIt: () -> Unit, onMove: () -> Unit) {
    Dialog(onDismissRequest = onGotIt, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color(0x99020808)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(XpTokens.Surface1)
                    .border(0.5.dp, XpTokens.Hair2, RoundedCornerShape(18.dp))
                    .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 18.dp),
            ) {
                IconBadge()
                Spacer(Modifier.height(16.dp))
                Text(
                    "Saved to Quick",
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 19.sp, fontWeight = FontWeight.SemiBold),
                    color = XpTokens.Ink,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "This note will disappear in 24 hours. Tap Keep on any quick note to move it to your regular notes.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, lineHeight = 20.sp),
                    color = XpTokens.Ink2,
                )
                Spacer(Modifier.height(18.dp))
                CountdownPreview(expiresAt = d.expiresAt)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton("Move to Notes", filled = false, onClick = onMove, modifier = Modifier.weight(1f))
                    ActionButton("Got it", filled = true, onClick = onGotIt, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun IconBadge() {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0x1A5EEAD4))
            .border(0.5.dp, XpTokens.Teal, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_clock),
            contentDescription = null,
            tint = XpTokens.Teal,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun CountdownPreview(expiresAt: Long) {
    val now = System.currentTimeMillis()
    val remaining = (expiresAt - now).coerceAtLeast(0)
    val left = remainingLabel(remaining)
    val expiresFmt = DateTimeFormatter.ofPattern("'expires tomorrow,' h:mm a")
    val expiresText = Instant.ofEpochMilli(expiresAt).atZone(ZoneId.systemDefault()).format(expiresFmt)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x0A5EEAD4))
            .border(0.5.dp, XpTokens.Hair, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(11.dp).clip(CircleShape).background(XpTokens.Teal.copy(alpha = 0.25f)))
        Spacer(Modifier.width(10.dp))
        Text("$left left", style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp), color = XpTokens.Teal)
        Spacer(Modifier.weight(1f))
        Text(expiresText, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp), color = XpTokens.Ink3)
    }
}

@Composable
private fun ActionButton(label: String, filled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (filled) XpTokens.Teal else Color.Transparent)
            .border(0.5.dp, if (filled) Color.Transparent else XpTokens.Hair2, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, fontWeight = if (filled) FontWeight.SemiBold else FontWeight.Medium),
            color = if (filled) XpTokens.OnTeal else XpTokens.Ink2,
        )
    }
}

@Composable
fun ClearAllDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color(0x99020808)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(XpTokens.Surface1)
                    .border(0.5.dp, XpTokens.Hair2, RoundedCornerShape(18.dp))
                    .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 18.dp),
            ) {
                Text("Clear all quick notes?", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold), color = XpTokens.Ink)
                Spacer(Modifier.height(8.dp))
                Text("Everything in Quick will be deleted. This can't be undone.", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, lineHeight = 20.sp), color = XpTokens.Ink2)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton("Cancel", filled = false, onClick = onDismiss, modifier = Modifier.weight(1f))
                    ActionButton("Clear", filled = true, onClick = onConfirm, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
