import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs

const val TRIANGLE_PARALLEL_RAY_THRESHOLD = 0.05

@Stable
class TriangleForRender(
    indices: TriangleIndices,
    points: List<XYZ>
) {
    private val a = points[indices.ai]
    private val b = points[indices.bi]
    private val c = points[indices.ci]

    private val bMinusA = b - a
    private val cMinusB = c - b
    private val aMinusC = a - c

    private val normal = bMinusA `×` (c - a)
    private val normalDotA = normal `•` a

    private val bounds = Rect(
        Offset(minOf(a.x, b.x, c.x), minOf(a.y, b.y, c.y)),
        Offset(maxOf(a.x, b.x, c.x), maxOf(a.y, b.y, c.y))
    )

    val facingValue = normal.normalized().z * 0.5f + 0.5f

    fun overlappedBy(rect: Rect) = rect.overlaps(bounds)

    context(DrawScope)
    fun renderOutline(color: Color) {
        drawPoints(
            points = listOf(
                a.toOffset(), b.toOffset(),
                b.toOffset(), c.toOffset(),
                c.toOffset(), a.toOffset()
            ),
            pointMode = PointMode.Lines,
            color = color
        )
    }

    @Stable
    fun tForRayOrNull(start: XYZ, direction: XYZ): Float? {
        val nDotRayDirection = normal `•` direction

        if (abs(nDotRayDirection) < TRIANGLE_PARALLEL_RAY_THRESHOLD) return null

        val t = -((normal `•` start) - normalDotA) / nDotRayDirection
        val intersection = start + direction * t

        return t.takeUnless {
            normal `•` (bMinusA `×` (intersection - a)) < 0 ||
            normal `•` (cMinusB `×` (intersection - b)) < 0 ||
            normal `•` (aMinusC `×` (intersection - c)) < 0
        }
    }

    override fun toString() = "TriangleForRender($a, $b, $c)"
}
