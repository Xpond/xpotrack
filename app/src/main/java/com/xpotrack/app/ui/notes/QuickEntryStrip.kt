package com.xpotrack.app.ui.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun QuickEntryStrip(onCompose: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCompose)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lightning),
            contentDescription = null,
            tint = XpTokens.Teal,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "Quick",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = XpTokens.Ink,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "tap to jot — gone in 24h",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = XpTokens.Ink3,
            modifier = Modifier.weight(1f),
        )
    }
}
