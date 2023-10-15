package input

import androidx.compose.ui.input.key.KeyEvent

interface WorldInputTarget {
    fun integrateIntoKeysFlow(
        observeKeysPressed: (predicate: (KeyEvent) -> Boolean, action: (KeyEvent) -> Unit) -> Unit
    )
}