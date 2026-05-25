package com.xpotrack.app.ui.categories

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xpotrack.app.data.model.Category
import com.xpotrack.app.ui.components.SheetGrabber
import com.xpotrack.app.ui.theme.XpTokens
import kotlinx.coroutines.launch

enum class CategorySheetMode { Picker, Manager }

// Atomic state for "the sheet is open" — both fields are set in one write, so
// the visibility predicate flips exactly once per chip-tap.
data class CategoryRequest(
    val selectedId: Long,
    val onApply: (Long) -> Unit,
)

// One sheet, two content modes. The picker→manager transition is a content
// cross-fade inside the same ModalBottomSheet, so the user never sees a
// double-scrim or a mid-animation tear. All dismissals run sheetState.hide()
// before flipping the visibility flag, so the slide-down always plays in full.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySheet(
    visible: Boolean,
    mode: CategorySheetMode,
    categories: List<Category>,
    selectedId: Long,
    managerVm: CategoryManagerViewModel,
    onPick: (Long) -> Unit,
    onManage: () -> Unit,
    onCreated: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Drive the sheet's visibility from the parent flag. When `visible` flips
    // false we hide() first so the slide-down animation runs in full; the
    // unmount happens below once the sheet has settled to Hidden.
    LaunchedEffect(visible) {
        if (visible) sheetState.show()
        else if (sheetState.currentValue != SheetValue.Hidden) sheetState.hide()
    }
    if (!visible && sheetState.currentValue == SheetValue.Hidden) return

    // Hide-then-flip helper. Callers pass the state mutation to run once the
    // sheet has finished animating away — never flip parent state synchronously
    // mid-animation, that's what makes the sheet snap.
    val dismiss: (() -> Unit) -> Unit = { after ->
        scope.launch {
            sheetState.hide()
            after()
        }
    }

    ModalBottomSheet(
        onDismissRequest = { dismiss(onDismiss) },
        sheetState = sheetState,
        containerColor = XpTokens.Surface1,
        contentColor = XpTokens.Ink,
        dragHandle = { SheetGrabber() },
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        // AnimatedContent owns both the content cross-fade and the height
        // morph via SizeTransform — the sheet's outer container follows the
        // child's animated measure, so picker↔manager grows/shrinks smoothly
        // instead of snapping.
        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                (fadeIn(tween(140)) togetherWith fadeOut(tween(100)))
                    .using(SizeTransform(clip = false) { _, _ -> tween(200) })
            },
            label = "category-sheet-mode",
            modifier = Modifier.fillMaxWidth(),
        ) { current ->
            when (current) {
                CategorySheetMode.Picker -> PickerContent(
                    categories = categories,
                    selectedId = selectedId,
                    onPick = { id -> dismiss { onPick(id) } },
                    onManage = onManage,
                )
                CategorySheetMode.Manager -> ManagerContent(
                    vm = managerVm,
                    onCreated = { id -> dismiss { onCreated(id) } },
                )
            }
        }
    }
}

@Composable
internal fun SheetDivider() { Box(Modifier.fillMaxWidth().height(0.5.dp).background(XpTokens.Hair)) }
