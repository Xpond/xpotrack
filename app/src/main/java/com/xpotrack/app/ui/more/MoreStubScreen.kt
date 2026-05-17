package com.xpotrack.app.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun MoreStubScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(XpTokens.Bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PREFERENCES".uppercase(), style = MaterialTheme.typography.labelSmall, color = XpTokens.Ink3)
            Spacer(Modifier.height(12.dp))
            Text("Settings", style = MaterialTheme.typography.displayLarge, color = XpTokens.Ink)
            Spacer(Modifier.height(16.dp))
            Text("Coming later.", style = MaterialTheme.typography.bodyMedium, color = XpTokens.Ink3)
        }
    }
}
