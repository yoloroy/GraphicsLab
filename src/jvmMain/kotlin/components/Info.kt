package components

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun Info(visible: Boolean, close: () -> Unit) {
    if (!visible) return

    Dialog(
        visible = true,
        title = "Info",
        onCloseRequest = close
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                """
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
            """.trimIndent()
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = close) {
                Text("OK")
            }
        }
    }
}