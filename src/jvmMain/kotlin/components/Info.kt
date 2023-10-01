package components

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.MenuScope

interface Info {
    @Composable
    fun Dialog()

    context(MenuScope)
    @Composable
    fun MenuBarItem()
}

class ComposableInfo: Info {

    private var visible by mutableStateOf(false)

    @Composable
    override fun Dialog() {
        if (!visible) return

        Dialog(
            visible = true,
            title = "Info",
            onCloseRequest = { visible = false }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text("""
                    Dashed line – Path to nearest point
                
                    Shift + Primary click – Add nearest point to selection
                    Secondary click – Open context menu
                    Ctrl/⌘ + C – Copy figure
                    Ctrl/⌘ + V – Paste figure
                    ⌫ – Remove nearest point
                    ⎵ – Toggle connection pairwise between all selected points
                    W, A, S, D – move up, left, down, right accordingly
                    Q, E – rotate left, rotate right compass-like on XY
                    Z, X – rotate left, rotate right compass-like on YZ
                    C, V – rotate left, rotate right compass-like on ZY
                
                    1 – view XY face
                    2 – view YZ face
                    3 – view ZX face
                """.trimIndent())
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { visible = false }) {
                    Text("OK")
                }
            }
        }
    }

    fun integrateIntoKeysFlow(
        observeKeysPressed: (
            predicate: (KeyEvent) -> Boolean,
            action: (KeyEvent) -> Unit
        ) -> Unit
    ) {
        observeKeysPressed({ it.key == Key.I }) { visible = !visible }
    }

    context(MenuScope)
    @Composable
    override fun MenuBarItem() {
        Item(text = "Info", onClick = { visible = true })
    }
}
