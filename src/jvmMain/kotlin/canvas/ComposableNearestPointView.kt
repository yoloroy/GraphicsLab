package canvas

import points.NearestPoint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import input.Cursor

class ComposableNearestPointView(
    private val nearestPoint: NearestPoint,
    private val cursor: Cursor,
    private val points: CanvasPoints
): NearestPointView {
    context(DrawScope)
    override fun drawPathToCursor() {
        nearestPoint.index?.let { index ->
            drawLine(
                color = Color.Black,
                start = cursor.position,
                end = points.offsets[index],
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
            )
        }
    }
}
