package input

import points.PointsSelection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import util.isWinCtrlPressed

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
