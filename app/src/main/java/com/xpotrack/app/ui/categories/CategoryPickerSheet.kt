package com.xpotrack.app.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R
import com.xpotrack.app.data.model.Category
import com.xpotrack.app.ui.theme.XpTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    categories: List<Category>,
    selectedId: Long,
    onPick: (Long) -> Unit,
    onManage: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = XpTokens.Surface1,
        contentColor = XpTokens.Ink,
        dragHandle = { Grabber() },
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Box(Modifier.padding(start = 22.dp, top = 4.dp, bottom = 12.dp)) {
                Text("Category".uppercase(), style = MaterialTheme.typography.labelMedium, color = XpTokens.Ink3)
            }
            categories.forEachIndexed { i, c ->
                if (i > 0) Divider()
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(c.id) }
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(parseHexColor(c.colorHex)))
                    Spacer(Modifier.width(12.dp))
                    Text(c.name, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                        color = XpTokens.Ink, modifier = Modifier.weight(1f))
                    if (c.id == selectedId) {
                        Icon(painterResource(R.drawable.ic_check), "Selected",
                            tint = XpTokens.Teal, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Divider()
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onManage)
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(painterResource(R.drawable.ic_plus), null,
                    tint = XpTokens.TealDim, modifier = Modifier.size(13.dp))
                Text("Manage categories…", fontSize = 14.sp, color = XpTokens.TealDim, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun Grabber() {
    Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(width = 38.dp, height = 4.dp).clip(RoundedCornerShape(2.dp))
            .background(XpTokens.Ink3.copy(alpha = 0.35f)))
    }
}

@Composable
private fun Divider() { Box(Modifier.fillMaxWidth().height(0.5.dp).background(XpTokens.Hair)) }
