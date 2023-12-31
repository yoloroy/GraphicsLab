package canvas

import points.Points
import points.World
import points.XYZ
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import points.connections
import points.toCanvasXYZ
import util.retrieveIndices
import points.withoutPairsOf

class ComposableCanvasPoints(
    private val worldPoints: Points,
    private val world: World
): CanvasPoints {
    private val state by derivedStateOf(policy = neverEqualPolicy()) {
        val points = worldPoints.points.map { it.toCanvasXYZ(world) }
        val offsets = points.map { it.toOffset() }
        val triangles = worldPoints.triangles
            .filter { it.isFacingCamera(points) }
            .map { TriangleForRender(it, points) }
        val nonTriangleConnections = worldPoints.connections.asIterable() withoutPairsOf worldPoints.triangles

        State(points, offsets, triangles, nonTriangleConnections)
    }

    override val points by derivedStateOf { state.points }
    override val offsets by derivedStateOf { state.offsets }
    override val triangles by derivedStateOf { state.triangles }
    override val nonTriangleConnections by derivedStateOf { state.nonTriangleConnections }

    override val connections by derivedStateOf { worldPoints.connections }

    fun indicesOfContainingIn(rect: Rect) = offsets.withIndex().filter { it.value in rect }.retrieveIndices()

    private data class State(
        val points: List<XYZ>,
        val offsets: List<Offset>,
        val triangles: List<TriangleForRender>,
        val nonTriangleConnections: List<Pair<Int, Int>>
    )
}
