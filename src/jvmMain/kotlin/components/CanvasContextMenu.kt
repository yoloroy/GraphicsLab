package components

import androidx.compose.runtime.Composable

interface CanvasContextMenu {
    @Composable
    fun Area(content: @Composable () -> Unit)
}
