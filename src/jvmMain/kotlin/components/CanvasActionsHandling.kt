package components

import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.foundation.onClick
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.onPointerEvent

@Composable
fun Modifier.onCursorActions(
    cursorDragState: CursorDragState,
    onMove: (change: PointerInputChange) -> Unit,
    onPrimaryClick: () -> Unit
) = this
    .onPointerEvent(PointerEventType.Move) { onMove(currentEvent.changes.first()) }
    .onSelectArea(cursorDragState)
    .onClick(matcher = PointerMatcher.Primary) { onPrimaryClick() }

@Composable
private fun Modifier.onSelectArea(cursorDragState: CursorDragState): Modifier {
    return onDrag(
        onDragStart = cursorDragState::onDragStart,
        onDrag = cursorDragState::onDrag,
        onDragEnd = cursorDragState::onDragEnd
    )
}

@Composable
fun rememberCursorDragState(cursorOffset: Offset): CursorDragState {
    var dragging by remember { mutableStateOf(false) }

    return remember(dragging) {
        CursorDragState(
            cursorOffset,
            dragging,
            onDragProcessStart = { dragging = true },
            onDragProcessEnd = { dragging = false }
        )
    }
}

class CursorDragState(
    cursorOffset: Offset,
    val dragging: Boolean,
    private val onDragProcessStart: () -> Unit,
    private val onDragProcessEnd: () -> Unit
) {

    var end by mutableStateOf(cursorOffset)
    val start by mutableStateOf(cursorOffset)

    private var onDragEndCallback: ((start: Offset, end: Offset) -> Unit)? by mutableStateOf(null)

    val diff get() = end - start

    fun onDragStart(end: Offset) {
        onDragProcessStart()
        this.end = end
    }

    fun onDrag(offset: Offset) {
        end += offset
    }

    fun onDragEnd() {
        onDragProcessEnd()
        onDragEndCallback?.invoke(start, end)
    }

    fun observeOnDragEnd(callback: (start: Offset, end: Offset) -> Unit) {
        this.onDragEndCallback = callback
    }
}
