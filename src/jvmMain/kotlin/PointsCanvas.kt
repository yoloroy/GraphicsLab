import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import components.CursorInput
import components.diff
import components.handleCursorInput

interface PointsCanvas {
    @Composable
    fun View(modifier: Modifier)
}

class ComposablePointsCanvas(
    private val isTransparentBuild: Boolean,
    private val world: World,
    private val cursorInput: CursorInput,
    private val points: CanvasPoints,
    private val selection: SelectedPoints,
    private val nearestPoint: NearestPoint
): PointsCanvas {

    @Composable
    override fun View(modifier: Modifier) {
        Canvas(
            modifier
                .background(if (isTransparentBuild) Color(0x44ffffff) else Color.White)
                .handleCursorInput(cursorInput)
        ) {
            world.drawCoordinateAxes()

            for ((ai, bi) in points.connections) {
                drawLine(Color.Black, points.points[ai], points.points[bi])
            }

            for (point in points.points) {
                drawCircle(Color.Black, 2F, point)
            }

            for (index in selection.selected) {
                val isSelectionPosOffset = cursorInput.dragging && cursorInput.mode is CursorInput.Drag
                val point = when (true) {
                    isSelectionPosOffset -> points.points[index] + cursorInput.dragState.diff
                    else -> points.points[index]
                }
                drawCircle(Color.Black, 4F, point, style = Stroke(1f))
            }

            if (!cursorInput.dragging) {
                nearestPoint.drawPathToCursor()
            }
            cursorInput.draw()
        }
    }
}
