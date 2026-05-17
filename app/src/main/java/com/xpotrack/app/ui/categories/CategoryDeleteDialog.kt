package com.xpotrack.app.ui.categories

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xpotrack.app.ui.theme.XpTokens

// Confirm-then-uncategorize dialog. Built with Dialog (not AlertDialog) so
// chrome matches the rest of the app — no Material defaults bleeding through.
@Composable
fun DeleteDialog(p: PendingDelete, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(XpTokens.Surface1)
                .border(0.5.dp, XpTokens.Hair2, RoundedCornerShape(18.dp))
                .padding(horizontal = 22.dp, vertical = 22.dp),
        ) {
            Text("Delete \"${p.name}\"?", color = XpTokens.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Text(summary(p), color = XpTokens.Ink2, fontSize = 13.5.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn("Cancel", filled = false, onClick = onCancel, modifier = Modifier.weight(1f))
                Btn("Delete", filled = true,  onClick = onConfirm, modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun summary(p: PendingDelete): String {
    return when (p.noteCount) {
        0 -> "This category is empty."
        1 -> "1 note will become uncategorized."
        else -> "${p.noteCount} notes will become uncategorized."
    }
}

@Composable
private fun Btn(label: String, filled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.height(44.dp).clip(RoundedCornerShape(12.dp))
            .background(if (filled) XpTokens.Teal else XpTokens.Surface2)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = if (filled) XpTokens.OnTeal else XpTokens.Ink2)
    }
}
