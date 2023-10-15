package components

import points.Points
import points.PointsSelectionAwareOfNearestPoint
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import input.CursorInput
import input.CursorXYZ
import points.isConnected
import points.isNotEmpty

class ComposableCanvasContextMenu(
    private val selection: PointsSelectionAwareOfNearestPoint,
    private val cursorInput: CursorInput,
    private val cursor: CursorXYZ,
    private val points: Points
): CanvasContextMenu {

    @Composable
    override fun Area(content: @Composable () -> Unit) {
        val contextMenuState = remember { ContextMenuState() }
        val savedCursorXYZ = remember(contextMenuState.status) { cursor.position } // saves when menu opens

        ContextMenuArea(
            items = {
                buildList {
                    val affected = selection.selected

                    add(ContextMenuItem("Create Point") { points.append(savedCursorXYZ) })

                    if (selection.isNotEmpty()) {
                        add(ContextMenuItem("Remove", selection::remove))
                        add(ContextMenuItem("Drag points") { cursorInput.mode = cursorInput.Drag() })
                    }
                    if (affected.size == 2 && points.isConnected(affected[0], affected[1])) {
                        add(ContextMenuItem("Split in half", selection::splitInHalf))
                    }
                    if (affected.size >= 2) {
                        add(ContextMenuItem("Connect", selection::connect))
                        add(ContextMenuItem("Disconnect", selection::disconnect))
                    }
                    if (affected.size == 3) {
                        add(ContextMenuItem("Create Polygon", selection::createPolygon))
                    }
                    if (selection.manuallySelected.isNotEmpty()) {
                        add(ContextMenuItem("Clear selection", selection::clear))
                    }
                }
            },
            state = contextMenuState,
            content = content
        )
    }
}
