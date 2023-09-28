import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Rect
import util.retrieveIndices

class CanvasPoints(
    private val worldPoints: Points,
    private val world: World
) {
    val points by derivedStateOf { worldPoints.points.map { it.toCanvas(world) } }
}

fun CanvasPoints.indicesOfContainingIn(rect: Rect) = points.withIndex().filter { it.value in rect }.retrieveIndices()
