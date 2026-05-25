package com.xpotrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun SheetGrabber() {
    Box(
        Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(width = 38.dp, height = 4.dp).clip(RoundedCornerShape(2.dp))
                .background(XpTokens.Ink3.copy(alpha = 0.35f)),
        )
    }
}
