package com.xpotrack.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun QuickEntryStrip(count: Int = 0, oldestLeft: String? = null, onClick: () -> Unit = {}) {
    val subtitle = when {
        count == 0 -> "Disappearing notes · tap to jot"
        oldestLeft == null -> "Disappearing notes"
        else -> "Disappearing notes · oldest expires in $oldestLeft"
    }
    val pillText = if (count == 0) "24H" else "$count · 24H"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 18.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(0.5.dp, Color(0x595EEAD4), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(0x145EEAD4)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lightning),
                contentDescription = null,
                tint = XpTokens.Teal,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Quick",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                    color = XpTokens.Ink,
                )
                Spacer(Modifier.width(8.dp))
                CountPill(text = pillText)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.5.sp),
                color = XpTokens.Ink3,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = XpTokens.Ink3,
            modifier = Modifier.size(13.dp),
        )
    }
}

@Composable
private fun CountPill(text: String) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0x145EEAD4))
            .padding(horizontal = 7.dp, vertical = 1.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = XpTokens.TealDim,
        )
    }
}
