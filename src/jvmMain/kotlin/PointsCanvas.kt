import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    private val cursorInput: CursorInput,
    private val points: CanvasPoints,
    private val selection: SelectedPoints,
    private val nearestPoint: NearestPoint,
    trianglesComponent: PointsCanvas
): PointsCanvas {

    var trianglesComponent by mutableStateOf(trianglesComponent)

    @Composable
    override fun View(modifier: Modifier) {
        Box(
            modifier
                .background(if (isTransparentBuild) Color(0x44ffffff) else Color.White)
                .handleCursorInput(cursorInput)
        ) {
            trianglesComponent.View(Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize()) {
                // TODO move
                for (index in selection.selected) {
                    val isSelectionPosOffset = cursorInput.dragging && cursorInput.mode is CursorInput.Drag
                    val point = when (true) {
                        isSelectionPosOffset -> points.offsets[index] + cursorInput.dragState.diff
                        else -> points.offsets[index]
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
}
