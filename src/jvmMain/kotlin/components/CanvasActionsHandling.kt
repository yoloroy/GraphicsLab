package components

import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.onClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

interface CursorActionsHandler {

    fun onMove(change: PointerInputChange)

    fun onScroll(change: PointerInputChange)

    fun onPrimaryClick(pointerOffset: Offset)

    fun onToggleMagnetizingAction()

    fun onCanvasSizeUpdate(coordinates: LayoutCoordinates)
}

fun Modifier.onCursorActions(handler: CursorActionsHandler) = this
    .onPointerEvent(PointerEventType.Move) { handler.onMove(it.changes.first()) }
    .onPointerEvent(PointerEventType.Scroll) { handler.onScroll(it.changes.first()) }
    .pointerInput(Unit) { detectTapGestures(onTap = handler::onPrimaryClick) }
    .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary), onClick = handler::onToggleMagnetizingAction)
    .onGloballyPositioned(handler::onCanvasSizeUpdate)
