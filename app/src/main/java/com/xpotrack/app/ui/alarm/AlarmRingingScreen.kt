package com.xpotrack.app.ui.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun AlarmRingingScreen(
    title: String,
    time: String,
    repeat: String,
    note: NoteSnippet?,
    onSnooze: (Int) -> Unit,
    onDone: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    0f to Color(0xFF0C2A26),
                    0.5f to XpTokens.Bg,
                    1f to XpTokens.Bg,
                )
            )
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AlarmTopRow(time = time)
            Spacer(Modifier.height(36.dp))
            AlarmTitleBlock(title = title, repeat = repeat)
            if (note != null) {
                Spacer(Modifier.height(20.dp))
                NoteCard(note = note)
            }
            Spacer(Modifier.height(36.dp))
            SnoozeList(onSnooze = onSnooze, baseTime = time)
            Spacer(Modifier.height(24.dp))
            HoldToMarkDone(onDone = onDone)
        }
    }
}
