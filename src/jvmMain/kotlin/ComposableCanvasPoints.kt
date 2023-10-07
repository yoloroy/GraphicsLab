import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import util.retrieveIndices
import util.toIntArray

interface CanvasPoints {
    val points: List<Offset>
    val connections: Sequence<Pair<Int, Int>>

    val triangles: List<Triple<Int, Int, Int>>
    val nonTriangles: List<Int>    
}

class ComposableCanvasPoints(
    private val worldPoints: Points,
    private val world: World
): CanvasPoints {
    override val points by derivedStateOf { worldPoints.points.map { it.toCanvas(world) } }
    override val connections get() = worldPoints.connections

    override val triangles by derivedStateOf {
        worldPoints.triangles.filter { (ai, bi, ci) -> // TODO move math of triangle to separate Triangle class
            val a = points[ai]
            val b = points[bi]
            val c = points[ci]

            val dx1 = b.x - a.x
            val dx2 = c.x - b.x
            val dy1 = b.y - a.y
            val dy2 = c.y - b.y

            dx1 * dy2 - dx2 * dy1 >= 0
        }
    }
    override val nonTriangles by derivedStateOf {
        points.indices.toIntArray().toList() - triangles.flatMap { listOf(it.first, it.second, it.third) }
    }
}

fun ComposableCanvasPoints.indicesOfContainingIn(rect: Rect) = points.withIndex().filter { it.value in rect }.retrieveIndices()
