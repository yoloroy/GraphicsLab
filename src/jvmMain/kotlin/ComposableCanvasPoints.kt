import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import util.retrieveIndices

interface CanvasPoints {
    val points: List<Offset>
    val connections: Sequence<Pair<Int, Int>>
}

class ComposableCanvasPoints(
    private val worldPoints: Points,
    private val world: World
): CanvasPoints {
    override val points by derivedStateOf { worldPoints.points.map { it.toCanvas(world) } }
    override val connections get() = worldPoints.connections
}

fun ComposableCanvasPoints.indicesOfContainingIn(rect: Rect) = points.withIndex().filter { it.value in rect }.retrieveIndices()
