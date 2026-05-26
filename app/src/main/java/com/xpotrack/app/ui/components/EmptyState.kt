package com.xpotrack.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun EmptyState(title: String, helper: String, modifier: Modifier = Modifier) {
    // Hand-rolled blink driven by withFrameMillis. Vivo/Funtouch pauses
    // InfiniteTransition (same family as the CircularProgressIndicator
    // throttle), so the caret never animated on the test device. The
    // Choreographer-backed withFrameMillis loop can't be throttled the same way.
    var caretOn by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        var lastFlipMs = 0L
        while (true) {
            withFrameMillis { now ->
                if (now - lastFlipMs >= 500L) {
                    caretOn = !caretOn
                    lastFlipMs = now
                }
            }
        }
    }
    Column(
        modifier.fillMaxWidth().padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = XpTokens.Ink2, fontSize = 14.sp)
            Text(
                " |",
                color = if (caretOn) XpTokens.Teal else XpTokens.Teal.copy(alpha = 0f),
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(helper, color = XpTokens.Ink3, fontSize = 12.sp)
    }
}
