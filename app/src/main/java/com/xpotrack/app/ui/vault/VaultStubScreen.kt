package com.xpotrack.app.ui.vault

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun VaultStubScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF050D0C),
                    0.6f to XpTokens.Bg,
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("VAULT".uppercase(), style = MaterialTheme.typography.labelSmall, color = XpTokens.TealDim)
            Spacer(Modifier.height(12.dp))
            Text("Locked notes", style = MaterialTheme.typography.displayLarge, color = XpTokens.Ink)
            Spacer(Modifier.height(16.dp))
            Text("Coming in milestone-3 of the design plan.", style = MaterialTheme.typography.bodyMedium, color = XpTokens.Ink3)
        }
    }
}
