import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

interface PointsSelection {

    val selected: List<Int>

    fun select(vararg indices: Int)

    fun selectOnly(vararg indices: Int)

    fun deselect(vararg indices: Int)

    fun toggleSelection(vararg indices: Int)

    fun clear()

    fun remove()

    fun connect()

    fun disconnect()

    fun toggleConnection()
}

interface PointsSelectionAwareOfNearestPoint: PointsSelection {
    val manuallySelected: List<Int>
}

class ComposablePointsSelection(private val points: Points): PointsSelection {

    override var selected by mutableStateOf(listOf<Int>()) // TODO Set

    override fun select(vararg indices: Int) {
        selected += indices.asList()
        selected = selected.distinct()
    }

    override fun selectOnly(vararg indices: Int) {
        selected = indices.toList().distinct()
    }

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
}

class ComposablePointsSelectionAwareOfNearestPoint(
    private val nearestPoint: NearestPoint,
    private val manualSelection: PointsSelection
): PointsSelectionAwareOfNearestPoint {

    override val manuallySelected by manualSelection::selected
    override val selected get() = manuallySelected.ifEmpty { listOfNotNull(nearestPoint.index) }

    override fun select(vararg indices: Int) = manualSelection.select(*indices)

    override fun selectOnly(vararg indices: Int) = manualSelection.selectOnly(*indices)

    override fun deselect(vararg indices: Int) = manualSelection.deselect(*indices)

    override fun toggleSelection(vararg indices: Int) = manualSelection.toggleSelection(*indices)

    override fun clear() = manualSelection.clear()

    override fun remove() = manualSelection.remove()

    override fun connect() = manualSelection.connect()

    override fun disconnect() = manualSelection.disconnect()

    override fun toggleConnection() = manualSelection.toggleConnection()
}

fun PointsSelection.isNotEmpty() = selected.isNotEmpty()
