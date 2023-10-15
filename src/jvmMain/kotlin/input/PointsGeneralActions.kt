package input

import points.ComposablePoints
import points.PointsSelection
import points.TriangleIndices
import points.XYZ
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import components.Failures
import points.connections
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import util.isWinCtrlPressed

class PointsGeneralActions(
    private val points: ComposablePoints, // TODO refactor
    private val failures: Failures,
    private val clipboardManager: ClipboardManager,
    private val selection: PointsSelection
) {
    fun copy() {
        try {
            val encoded = Json.encodeToString(
                State(
                points.points,
                points.connections.flatMap { (ai, bi) -> listOf(ai, bi) }.toList()
            )
            )
            clipboardManager.setText(AnnotatedString(encoded))
        } catch (e: Exception) {
            failures.logException(e.message ?: "Uncaught exception")
        }
    }

    fun paste() {
        try {
            val loadedState = Json.decodeFromString<State>(clipboardManager.getText()?.text ?: State.EMPTY_JSON)

            if (loadedState.connections.any { it !in loadedState.points.indices }) {
                throw IllegalStateException("Bad indices in points.getConnections of pasted Json")
            }

            points.points = loadedState.points.toMutableList()
            points.adjacencyMatrix = MutableList(points.points.size) { MutableList(points.points.size) { false } }.apply {
                loadedState.connections.chunked(2).forEach { (ai, bi) ->
                    this[ai][bi] = true
                    this[bi][ai] = true
                }
            }
            points.triangles = mutableListOf<TriangleIndices>().apply {
                // [..., 0,1, 1,2, 2,0, ...] - it is the triangle
                var i = 0
                while (i + 5 in loadedState.connections.indices) {
                    val a1 = loadedState.connections[i + 0]
                    val b2 = loadedState.connections[i + 1]
                    val b1 = loadedState.connections[i + 2]
                    val c2 = loadedState.connections[i + 3]
                    val c1 = loadedState.connections[i + 4]
                    val a2 = loadedState.connections[i + 5]

                    if (a1 == a2 && b1 == b2 && c1 == c2) {
                        add(TriangleIndices(a1, b1, c1))
                        i += 5
                    }
                    i++
                }
            }
        } catch (e: SerializationException) {
            failures.logMistake("You did not pasted save")
        } catch (e: IllegalArgumentException) {
            failures.logException("Pasted save is not valid: ${e.message}")
        }
    }

    fun clear() {
        selection.clear()
        points.clear()
    }

    @Serializable
    private data class State(
        val points: List<XYZ>,
        val connections: List<Int>
    ) {
        companion object {
            const val EMPTY_JSON = "{ \"points\" = [], \"points.getConnections\" = [] }"
        }
    }
}

fun PointsGeneralActions.integrateIntoKeysFlow(
    observeKeysPressed: (
        predicate: (KeyEvent) -> Boolean,
        action: (KeyEvent) -> Unit
    ) -> Unit
) {
    observeKeysPressed.invoke({ it.isWinCtrlPressed && it.key == Key.C }) { copy() }
    observeKeysPressed.invoke({ it.isWinCtrlPressed && it.key == Key.V }) { paste() }
}
