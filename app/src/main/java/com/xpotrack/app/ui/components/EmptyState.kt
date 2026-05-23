package com.xpotrack.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun EmptyState(title: String, helper: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "caret")
    val caretAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                1f at 0 using LinearEasing
                1f at 500 using LinearEasing
                0f at 500 using LinearEasing
                0f at 999 using LinearEasing
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "caretAlpha",
    )
    Column(
        modifier.fillMaxWidth().padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = XpTokens.Ink2, fontSize = 14.sp)
            Text(
                " |",
                color = XpTokens.Teal.copy(alpha = caretAlpha),
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(helper, color = XpTokens.Ink3, fontSize = 12.sp)
    }
}
