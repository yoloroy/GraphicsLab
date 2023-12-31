package points

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import canvas.ComposableCanvasPoints
import components.Failures
import input.Cursor
import util.distanceTo
import util.indexOfMinBy
import util.takeIfNotEmpty

interface NearestPoint {
    val index: Int?

    fun select()

    fun selectOnly()

    fun deselect()

    fun toggleSelection()
}

class ComposableNearestPoint(
    private val points: ComposableCanvasPoints,
    private val cursor: Cursor,
    private val selection: PointsSelection,
    private val failures: Failures
): NearestPoint {

    override val index by derivedStateOf {
        points.offsets
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

    private fun onPointDoesNotExist() {
        failures.logMistake("Nearest point does not exist therefore, action cannot be performed")
    }
}
