package canvas

import points.World
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class WireframeComponent(
    private val points: CanvasPoints,
    private val world: World
): PointsCanvas {

    @Composable
    override fun View(modifier: Modifier) {
        Canvas(modifier) {
            world.drawCoordinateAxes()

            for (triangle in points.triangles) {
                triangle.renderOutline(Color.Black)
            }

            for ((ai, bi) in points.nonTriangleConnections) {
                drawLine(Color.Red, points.offsets[ai], points.offsets[bi])
            }

            for (point in points.offsets) {
                drawCircle(Color.Black, 2F, point)
            }
        }
    }
}
