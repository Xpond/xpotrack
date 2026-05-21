package com.xpotrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun ConfirmDeleteDialog(
    title: String,
    subject: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(XpTokens.Surface1)
                .border(0.5.dp, XpTokens.Hair2, RoundedCornerShape(16.dp))
                .padding(20.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = XpTokens.Ink)
            Spacer(Modifier.height(10.dp))
            Text("“$subject”", color = XpTokens.Ink2, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text("This can't be undone.", color = XpTokens.Ink3, fontSize = 12.sp)
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DialogBtn(label = "Cancel", color = XpTokens.Ink2, onClick = onCancel)
                Spacer(Modifier.width(8.dp))
                DialogBtn(label = "Delete", color = XpTokens.Danger, onClick = onConfirm, bold = true)
            }
        }
    }
}

@Composable
private fun DialogBtn(label: String, color: Color, onClick: () -> Unit, bold: Boolean = false) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = color,
            fontSize = 14.sp,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}
