import androidx.compose.runtime.*
import components.Failures
import util.combinationsOfPairs

interface Points {
    val points: List<XYZ>
    val adjacencyMatrix: List<List<Boolean>>
    val triangles: List<Triple<Int, Int, Int>>

    fun append(xyz: XYZ)

    fun removeAt(index: Int)

    fun clear()

    fun setConnection(ai: Int, bi: Int, value: Boolean)

    fun transform(indices: Iterable<Int>, transform: (XYZ) -> XYZ)
}

class ComposablePoints(private val failures: Failures): Points {

    override var points by mutableStateOf(mutableListOf<XYZ>(), neverEqualPolicy())
    override var adjacencyMatrix by mutableStateOf(mutableListOf<MutableList<Boolean>>(), neverEqualPolicy())
    override var triangles by mutableStateOf(mutableListOf<Triple<Int, Int, Int>>(), neverEqualPolicy())

    override fun setConnection(ai: Int, bi: Int, value: Boolean) {
        if (ai !in points.indices) {
            failures.logException("Point[$ai] does not exist")
            return
        }
        if (bi !in points.indices) {
            failures.logException("Point[$bi] does not exist")
            return
        }

        adjacencyMatrix = adjacencyMatrix.apply {
            this[ai][bi] = value
            this[bi][ai] = value
        }
    }

    override fun append(xyz: XYZ) {
        points = points.apply { add(xyz) }
        adjacencyMatrix = adjacencyMatrix.apply {
            forEach { it += false }
            this += MutableList(points.size) { false }
        }
    }

    override fun removeAt(index: Int) {
        if (index !in points.indices) {
            failures.logException("Point[$index] does not exist to be removed")
            return
        }

        points = points.apply { removeAt(index) }
        adjacencyMatrix = adjacencyMatrix.apply {
            removeAt(index)
            forEach { it.removeAt(index) }
        }
    }

    override fun clear() {
        points = mutableListOf()
        adjacencyMatrix = mutableListOf()
    }

    override fun transform(indices: Iterable<Int>, transform: (XYZ) -> XYZ) {
        points = points.apply {
            for ((i, point) in points.withIndex()) if (i in indices) {
                points[i] = transform(point)
            }
        }
    }
}

val Points.connections get() = adjacencyMatrix
    .asSequence()
    .flatMapIndexed { ai, connections ->
        connections
            .asSequence()
            .withIndex()
            .filter { it.value }
            .map { (bi, _) -> ai to bi }
    }

fun Points.connect(ai: Int, bi: Int) = setConnection(ai, bi, true)

fun Points.disconnect(ai: Int, bi: Int) = setConnection(ai, bi, false)

fun Points.connectAll(indices: List<Int>) {
    for ((ai, bi) in indices.combinationsOfPairs()) {
        connect(ai, bi)
    }
}

fun Points.disconnectAll(indices: List<Int>) {
    for ((ai, bi) in indices.combinationsOfPairs()) {
        disconnect(ai, bi)
    }
}

fun Points.toggleConnection(ai: Int, bi: Int) {
    if (adjacencyMatrix[ai][bi]) {
        disconnect(ai, bi)
    } else {
        connect(ai, bi)
    }
}

fun Points.toggleConnections(indices: List<Int>) {
    for ((ai, bi) in indices.combinationsOfPairs()) {
        toggleConnection(ai, bi)
    }
}

fun Points.removeAll(indices: Iterable<Int>) {
    for ((i, iToRemove) in indices.withIndex()) {
        val removingOffset = -indices.asSequence().take(i).count { it < iToRemove }
        removeAt(iToRemove + removingOffset)
    }
}

fun Points.splitInHalf(ai: Int, bi: Int) {
    disconnect(ai, bi)
    append(listOf(points[ai], points[bi]).average())
    val i = points.lastIndex
    connect(ai, i)
    connect(bi, i)
}

fun Points.isConnected(ai: Int, bi: Int) = adjacencyMatrix[ai][bi]
