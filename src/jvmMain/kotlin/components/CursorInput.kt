package components

import CanvasPoints
import ComposableCursor
import ComposablePoints
import NearestPoint
import PointsSelectionAwareOfNearestPoint
import World
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.foundation.onClick
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.onPointerEvent
import indicesOfContainingIn
import toCanvas
import toWorldXYZ
import util.areaOf
import util.drawArrow
import kotlin.math.max
import kotlin.math.min

class CursorInput( // TODO refactor too many dependencies
    private val cursor: ComposableCursor,
    private val selection: PointsSelectionAwareOfNearestPoint,
    private val isShiftPressedProvider: () -> Boolean,
    private val nearestPoint: NearestPoint,
    private val canvasPoints: CanvasPoints,
    private val points: ComposablePoints,
    private val world: World,
    val dragState: CursorDragState
) {
    var mode: DragActionMode by mutableStateOf(Selection())
    val dragging get() = dragState.dragging

    init {
        dragState.onDragEndCallback = ::onDragEnd
    }

    fun onPrimaryClick() = mode.onPrimaryClick()

    fun onMove(change: PointerInputChange) { cursor.position = change.position }

    fun onDragEnd(start: Offset, end: Offset) = mode.onDragEnd(start, end)

    context(DrawScope)
    fun draw() {
        dragState.ifDragging { start, end -> mode.draw(start, end) }
    }

    sealed class DragActionMode {
        abstract fun onPrimaryClick()

        abstract fun onDragEnd(start: Offset, end: Offset)

        context(DrawScope)
        abstract fun draw(start: Offset, end: Offset)
    }

    inner class Selection: DragActionMode() {
        override fun onPrimaryClick() {
            if (isShiftPressedProvider()) {
                nearestPoint.toggleSelection()
            } else {
                selection.clear()
                if (selection.manuallySelected.isEmpty() && nearestPoint.index != null) {
                    nearestPoint.selectOnly()
                }
            }
        }

        override fun onDragEnd(start: Offset, end: Offset) {
            val selected = canvasPoints.indicesOfContainingIn(Rect.areaOf(start, end))
            if (isShiftPressedProvider()) {
                selection.toggleSelection(*selected.toIntArray())
            } else {
                selection.selectOnly(*selected.toIntArray())
            }
        }

        context(DrawScope)
        override fun draw(start: Offset, end: Offset) {
            val topLeft = Offset(min(start.x, end.x), min(start.y, end.y))
            val bottomRight = Offset(max(start.x, end.x), max(start.y, end.y))
            val size = (bottomRight - topLeft).run { Size(x, y) }

            drawCircle(Color.Black, 4f, start, 0.4f)
            drawCircle(Color.Black, 4f, end, 0.4f)

            drawRect(Color.Black, topLeft, size, 0.4f, Stroke(1f))
        }
    }

    inner class Drag: DragActionMode() {
        override fun onPrimaryClick() {
            selection.clear()
            mode = Selection()
        }

        override fun onDragEnd(start: Offset, end: Offset) {
            val diff = end.toWorldXYZ(world) - start.toWorldXYZ(world)
            points.transform(selection.selected) { it + diff }
        }

        context(DrawScope)
        override fun draw(start: Offset, end: Offset) {
            val startXYZ = start.toWorldXYZ(world)
            val endXYZ = end.toWorldXYZ(world)
            val diff = endXYZ - startXYZ

            val xShiftEnd = (startXYZ + XYZ.ZERO.copy(x = diff.x)).toCanvas(world)
            val yShiftEnd = (startXYZ + XYZ.ZERO.copy(y = diff.y)).toCanvas(world)
            val zShiftEnd = (startXYZ + XYZ.ZERO.copy(z = diff.z)).toCanvas(world)

            drawArrow(Color.Black, start, end)
            drawArrow(Color.Red, start, xShiftEnd)
            drawArrow(Color.Blue, start, yShiftEnd)
            drawArrow(Color.Green, start, zShiftEnd)
        }
    }
}

@Composable
fun Modifier.handleCursorInput(cursorInput: CursorInput) = this
    .onPointerEvent(PointerEventType.Move) { cursorInput.onMove(currentEvent.changes.first()) }
    .onSelectArea(cursorInput.dragState)
    .onClick(matcher = PointerMatcher.Primary, onClick = cursorInput::onPrimaryClick)

@Composable
private fun Modifier.onSelectArea(cursorDragState: CursorDragState) = this
    .onDrag(
        onDragStart = cursorDragState::onDragStart,
        onDrag = cursorDragState::onDrag,
        onDragEnd = cursorDragState::onDragEnd
    )

class CursorDragState {
    var end by mutableStateOf(Offset.Zero)
        private set
    var start by mutableStateOf(null as Offset?)
        private set

    val dragging: Boolean by derivedStateOf { start != null }

    // TODO refactor to partial construction?
    var onDragEndCallback: (start: Offset, end: Offset) -> Unit = { _, _ -> }

    fun onDragStart(offset: Offset) {
        end = offset
        start = offset
    }

    fun onDrag(offset: Offset) {
        end += offset
    }

    fun onDragEnd() {
        onDragEndCallback(start!!, end)
        start = null
    }

    fun ifDragging(block: (start: Offset, end: Offset) -> Unit) {
        val startNotNull = start ?: return
        block(startNotNull, end)
    }
}

val CursorDragState.diff get() = start?.let { end - it } ?: Offset.Unspecified
