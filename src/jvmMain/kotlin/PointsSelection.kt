import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import components.CursorInput
import util.isWinCtrlPressed
import util.toIntArray

interface SelectedPoints {
    val selected: List<Int>
}

interface SelectedPointsAwareOfNearestPoint: SelectedPoints {
    val manuallySelected: List<Int>
}

interface PointsSelection: SelectedPoints {

    fun createPolygon()

    fun select(vararg indices: Int)

    fun selectOnly(vararg indices: Int)

    fun selectAll()

    fun deselect(vararg indices: Int)

    fun toggleSelection(vararg indices: Int)

    fun clear()

    fun remove()

    fun connect()

    fun disconnect()

    fun toggleConnection()

    fun splitInHalf()
}

interface PointsSelectionAwareOfNearestPoint: PointsSelection, SelectedPointsAwareOfNearestPoint

class ComposablePointsSelection(private val points: Points): PointsSelection {

    override var selected by mutableStateOf(listOf<Int>()) // TODO Set

    override fun createPolygon() {
        require(selected.size == 3)
        points.createPolygon(selected[0], selected[1], selected[2])
    }

    override fun select(vararg indices: Int) {
        selected += indices.asList()
        selected = selected.distinct()
    }

    override fun selectOnly(vararg indices: Int) {
        selected = indices.toList().distinct()
    }

    override fun selectAll() = select(*points.points.indices.toIntArray())

    override fun deselect(vararg indices: Int) {
        selected -= indices.asList()
    }

    override fun toggleSelection(vararg indices: Int) {
        selected = selected - indices.toSet() + indices.filter { it !in selected }
    }

    override fun clear() {
        selected = emptyList()
    }

    override fun remove() = points.removeAll(selected)

    override fun connect() = points.connectAll(selected)

    override fun disconnect() = points.disconnectAll(selected)

    override fun toggleConnection() = points.toggleConnections(selected)

    override fun splitInHalf() {
        require(selected.size == 2)
        points.splitInHalf(selected[0], selected[1])
    }
}

class ComposablePointsSelectionAwareOfNearestPoint(
    private val nearestPoint: NearestPoint,
    private val manualSelection: PointsSelection
): PointsSelectionAwareOfNearestPoint {

    override val manuallySelected by manualSelection::selected
    override val selected get() = manuallySelected.ifEmpty { listOfNotNull(nearestPoint.index) }

    override fun createPolygon() = manualSelection.createPolygon()

    override fun select(vararg indices: Int) = manualSelection.select(*indices)

    override fun selectOnly(vararg indices: Int) = manualSelection.selectOnly(*indices)

    override fun selectAll() = manualSelection.selectAll()

    override fun deselect(vararg indices: Int) = manualSelection.deselect(*indices)

    override fun toggleSelection(vararg indices: Int) = manualSelection.toggleSelection(*indices)

    override fun clear() = manualSelection.clear()

    override fun remove() = manualSelection.remove()

    override fun connect() = manualSelection.connect()

    override fun disconnect() = manualSelection.disconnect()

    override fun toggleConnection() = manualSelection.toggleConnection()

    override fun splitInHalf() = manualSelection.splitInHalf()
}

class PointsSelectionFeaturingDeselection(
    private val base: PointsSelectionAwareOfNearestPoint
): PointsSelectionAwareOfNearestPoint by base {

    override fun remove() {
        base.remove()
        clear()
    }

    override fun disconnect() {
        base.disconnect()
        clear()
    }

    override fun connect() {
        base.connect()
        clear()
    }
}

class PointsSelectionFeaturingSwitchingCursorInputModeToSelection(
    private val base: PointsSelectionAwareOfNearestPoint,
    private val cursorInput: CursorInput
): PointsSelectionAwareOfNearestPoint by base {

    override fun clear() {
        base.clear()
        switchModeToSelection()
    }

    override fun connect() {
        base.connect()
        switchModeToSelection()
    }

    override fun disconnect() {
        base.disconnect()
        switchModeToSelection()
    }

    override fun toggleConnection() {
        base.toggleConnection()
        switchModeToSelection()
    }

    override fun remove() {
        base.remove()
        switchModeToSelection()
    }

    override fun splitInHalf() {
        base.splitInHalf()
        switchModeToSelection()
    }

    private fun switchModeToSelection() {
        cursorInput.mode = cursorInput.Selection()
    }
}

fun PointsSelection.isNotEmpty() = selected.isNotEmpty()

fun PointsSelection.integrateIntoKeysFlow(
    observeKeysPressed: (
        predicate: (KeyEvent) -> Boolean,
        action: (KeyEvent) -> Unit
    ) -> Unit
) {
    observeKeysPressed.invoke({ it.key == Key.Backspace }) { remove() }
    observeKeysPressed.invoke({ it.isWinCtrlPressed && it.key == Key.A }) { selectAll() }
    observeKeysPressed.invoke({ it.key == Key.Spacebar }) { toggleConnection() }
}
