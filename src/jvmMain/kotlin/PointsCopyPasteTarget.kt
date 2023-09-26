import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import components.Failures
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PointsCopyPasteTarget(
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
