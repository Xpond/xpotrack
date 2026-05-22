package com.xpotrack.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun EmptyState(title: String, helper: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = XpTokens.Ink2, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text(helper, color = XpTokens.Ink3, fontSize = 12.sp)
    }
}
