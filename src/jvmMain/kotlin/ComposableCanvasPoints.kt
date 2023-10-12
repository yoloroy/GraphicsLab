import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import util.retrieveIndices
import util.toIntArray

interface CanvasPoints {
    val points: List<XYZ>
    val offsets: List<Offset>
    val connections: Sequence<Pair<Int, Int>>

    val triangles: List<Triple<Int, Int, Int>>
    val nonTriangles: Set<Int>
}

class ComposableCanvasPoints(
    private val worldPoints: Points,
    private val world: World
): CanvasPoints {
    private val state by derivedStateOf {
        val points = worldPoints.points.map { it.toCanvasXYZ(world) }
        val offsets = points.map { it.toOffset() }
        val triangles = worldPoints.triangles.filter { (ai, bi, ci) -> // TODO move math of triangle to separate Triangle class
            val a = offsets[ai]
            val b = offsets[bi]
            val c = offsets[ci]

            val dx1 = b.x - a.x
            val dx2 = c.x - b.x
            val dy1 = b.y - a.y
            val dy2 = c.y - b.y

            dx1 * dy2 - dx2 * dy1 >= 0
        }
        val nonTriangles = offsets.indices.toIntArray().toSet() - worldPoints.triangles.flatMap { listOf(it.first, it.second, it.third) }.toSet()

        State(points, offsets, triangles, nonTriangles)
    }

    override val points get() = state.points
    override val offsets get() = state.offsets
    override val triangles get() = state.triangles
    override val nonTriangles get() = state.nonTriangles

    override val connections get() = worldPoints.connections

    private data class State(
        val points: List<XYZ>,
        val offsets: List<Offset>,
        val triangles: List<Triple<Int, Int, Int>>,
        val nonTriangles: Set<Int>
    )
}

fun ComposableCanvasPoints.indicesOfContainingIn(rect: Rect) = offsets.withIndex().filter { it.value in rect }.retrieveIndices()
