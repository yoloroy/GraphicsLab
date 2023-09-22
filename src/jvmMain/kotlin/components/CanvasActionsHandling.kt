package components

import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.onClick
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import kotlin.math.max
import kotlin.math.min

@Composable
fun Modifier.onCursorActions(
    onMove: (change: PointerInputChange) -> Unit,
    onScroll: (change: PointerInputChange) -> Unit,
    onPrimaryClick: () -> Unit,
    onSelectArea: (rect: Rect) -> Unit
) = this
    .onPointerEvent(PointerEventType.Move) { onMove(currentEvent.changes.first()) }
    .onPointerEvent(PointerEventType.Scroll) { onScroll(currentEvent.changes.first()) }
    .onClick(matcher = PointerMatcher.Primary) { onPrimaryClick() }
    .onSelectArea(onSelectArea)

@Composable
private fun Modifier.onSelectArea(onSelectArea: (rect: Rect) -> Unit): Modifier {
    var startOffset by remember { mutableStateOf<Offset?>(null) }

    return this
        .onPointerEvent(PointerEventType.Press) {
            if (!currentEvent.buttons.isPrimaryPressed) return@onPointerEvent
            startOffset = it.changes.first().position
        }
        .onPointerEvent(PointerEventType.Release) {
            if (!currentEvent.buttons.isPrimaryPressed) return@onPointerEvent
            val start = startOffset ?: return@onPointerEvent
            val end = currentEvent.changes.first().position
            onSelectArea(Rect(
                topLeft = Offset(min(start.x, end.x), min(start.y, end.y)),
                bottomRight = Offset(max(start.x, end.x), max(start.x, end.x))
            ))
        }
}
