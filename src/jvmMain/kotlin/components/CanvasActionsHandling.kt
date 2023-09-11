package components

import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.onClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

fun Modifier.onCursorActions(
    onMove: (change: PointerInputChange) -> Unit,
    onScroll: (change: PointerInputChange) -> Unit,
    onPrimaryClick: (pointerOffset: Offset) -> Unit,
    onToggleMagnetizingAction: () -> Unit,
    onCanvasSizeUpdate: (coordinates: LayoutCoordinates) -> Unit
) = this
    .onPointerEvent(PointerEventType.Move) { onMove(it.changes.first()) }
    .onPointerEvent(PointerEventType.Scroll) { onScroll(it.changes.first()) }
    .pointerInput(Unit) { detectTapGestures(onTap = onPrimaryClick) }
    .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary), onClick = onToggleMagnetizingAction)
    .onGloballyPositioned(onCanvasSizeUpdate)
