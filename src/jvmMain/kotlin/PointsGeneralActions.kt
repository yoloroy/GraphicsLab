import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.window.MenuScope
import components.Failures
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import util.isWinCtrlPressed

class PointsGeneralActions(
    private val points: ComposablePoints,
    private val failures: Failures,
    private val clipboardManager: ClipboardManager
) {
    fun copy() {
        try {
            val encoded = Json.encodeToString(State(
                points.points,
                points.connections.flatMap { (ai, bi) -> listOf(ai, bi) }.toList()
            ))
            clipboardManager.setText(AnnotatedString(encoded))
        } catch (e: Exception) {
            failures.logException(e.message ?: "Uncaught exception")
        }
    }

    fun paste() {
        try {
            val loadedState = Json.decodeFromString<State>(clipboardManager.getText()?.text ?: State.EMPTY_JSON)

            if (loadedState.connections.any { it !in loadedState.points.indices }) {
                throw IllegalStateException("Bad indices in connections of pasted Json")
            }

            points.points = loadedState.points.toMutableList()
            points.adjacencyMatrix = MutableList(points.points.size) { MutableList(points.points.size) { false } }.apply {
                loadedState.connections.chunked(2).forEach { (ai, bi) ->
                    this[ai][bi] = true
                    this[bi][ai] = true
                }
            }
        } catch (e: SerializationException) {
            failures.logMistake("You did not pasted save")
        } catch (e: IllegalArgumentException) {
            failures.logException("Pasted save is not valid: ${e.message}")
        }
    }

    fun clear() = points.clear()

    @Serializable
    private data class State(
        val points: List<XYZ>,
        val connections: List<Int>
    ) {
        companion object {
            const val EMPTY_JSON = "{ \"points\" = [], \"connections\" = [] }"
        }
    }
}

context(MenuScope)
@Composable
fun PointsGeneralActions.MenuBarItems() {
    Item(text = "Copy", onClick = ::copy)
    Item(text = "Paste", onClick = ::paste)
    Item(text = "Clear", onClick = ::clear)
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
