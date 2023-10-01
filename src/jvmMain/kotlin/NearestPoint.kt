import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import components.Failures
import util.distanceTo
import util.indexOfMinBy
import util.takeIfNotEmpty

interface NearestPoint {
    val index: Int?

    fun select()

    fun selectOnly()

    fun deselect()

    fun toggleSelection()

    context(DrawScope)
    fun drawPathToCursor()
}

class ComposableNearestPoint(
    private val points: ComposableCanvasPoints,
    private val cursor: Cursor,
    private val selection: PointsSelection,
    private val failures: Failures
): NearestPoint {

    override val index by derivedStateOf {
        points.points
            .takeIfNotEmpty()
            ?.indexOfMinBy { it distanceTo cursor.position }
    }

    override fun select() {
        index
            ?.let { selection.select(it) }
            ?: onPointDoesNotExist()
    }

    override fun selectOnly() {
        selection.clear()
        this.select()
    }

    override fun deselect() {
        index
            ?.let { selection.deselect(it) }
            ?: onPointDoesNotExist()
    }

    override fun toggleSelection() {
        index
            ?.let { selection.toggleSelection(it) }
            ?: onPointDoesNotExist()
    }

    context(DrawScope)
    override fun drawPathToCursor() {
        index?.let { index ->
            drawLine(
                color = Color.Black,
                start = cursor.position,
                end = points.points[index],
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
            )
        }
    }

    private fun onPointDoesNotExist() {
        failures.logMistake("Nearest point does not exist therefore, action cannot be performed")
    }
}
