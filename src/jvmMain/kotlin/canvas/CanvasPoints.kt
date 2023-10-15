package canvas

import points.XYZ
import androidx.compose.ui.geometry.Offset

interface CanvasPoints {
    val points: List<XYZ>
    val offsets: List<Offset>
    val connections: Sequence<Pair<Int, Int>>

    val triangles: List<TriangleForRender>
    val nonTriangleConnections: List<Pair<Int, Int>>
}
