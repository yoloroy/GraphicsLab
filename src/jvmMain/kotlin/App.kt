import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import components.*
import kotlinx.coroutines.flow.Flow
import util.isWinCtrlPressed

context(FrameWindowScope)
@Composable
@Preview
fun App(
    failures: Failures,
    world: World,
    worldAssignees: WorldAssignees,
    worldInputTarget: WorldInputTarget,
    points: Points,
    selection: PointsSelectionAwareOfNearestPoint,
    pointsCopyPasteTarget: PointsCopyPasteTarget,
    keysGlobalFlow: Flow<KeyEvent>,
    observeKeys: (predicate: (KeyEvent) -> Boolean, action: (KeyEvent) -> Unit) -> Unit,
    cursorInput: CursorInput,
    nearestPoint: NearestPoint,
    cursor: Cursor
) {
    val observeKeysPressed = { predicate: (KeyEvent) -> Boolean, action: (KeyEvent) -> Unit ->
        observeKeys({ it.type == KeyEventType.KeyDown && predicate(it) }, action)
    }

    // region ui
    var isInfoOpen by remember { mutableStateOf(false) }
    var isShiftPressed by remember { mutableStateOf(false) }

    // region world
    fun XYZ.toCanvas() = scaled(world.scale).`🔄Z`(world.xyRadians).`🔄X`(world.yzRadians).`🔄Y`(world.zxRadians).offset(world.offset).toOffset()

    val canvasPoints by remember { derivedStateOf { points.points.map { it.toCanvas() } } }

    fun Offset.toWorldXYZ() = toWorldXYZ(world.offset, world.scale, world.xyRadians, world.yzRadians, world.zxRadians)
    // endregion

    // region Cursor
    val contextMenuState = remember { ContextMenuState() }
    val contextMenuSavedCursorOffset = remember(contextMenuState.status) { cursor.position }
    // endregion

    val switchToDragModeAction = {
        cursorInput.mode = cursorInput.Drag()
    }

    val switchToSelectionModeAction = {
        cursorInput.mode = cursorInput.Selection()
    }

    fun deselectionContext(block: () -> Unit) = ({
        switchToSelectionModeAction()
        block()
        selection.clear()
    })

    val selectAllAction = {
        selection.select(*points.points.indices.toList().toIntArray())
    }

    val clearSelectionAction = {
        switchToSelectionModeAction()
        selection.clear()
    }

    val splitInHalfAction = {
        switchToSelectionModeAction()
        val (ai, bi) = selection.selected
        points.splitInHalf(ai, bi)
    }

    val createPointAction = { savedCursorOffset: Offset ->
        switchToSelectionModeAction()
        points.append(savedCursorOffset.toWorldXYZ())
    }

    val connectAction = deselectionContext { selection.connect() }

    val disconnectAction = deselectionContext { selection.disconnect() }

    val toggleConnectionAction = { selection.toggleConnection() }

    val removeAction = deselectionContext { selection.remove() }

    LaunchedEffect(Unit) {
        observeKeysPressed.invoke({ it.isWinCtrlPressed && it.key == Key.C }) { pointsCopyPasteTarget.copy() }
        observeKeysPressed.invoke({ it.isWinCtrlPressed && it.key == Key.V }) { pointsCopyPasteTarget.paste() }

        observeKeys.invoke({ it.key in listOf(Key.ShiftLeft, Key.ShiftRight) }) {
            isShiftPressed = it.type == KeyEventType.KeyDown
        }
        observeKeysPressed.invoke({ it.key == Key.Backspace }) { removeAction.invoke() }

        observeKeysPressed.invoke({ it.isWinCtrlPressed && it.key == Key.A }) { selectAllAction.invoke() }
        observeKeysPressed.invoke({ it.key == Key.Spacebar }) { toggleConnectionAction.invoke() }

        worldInputTarget.integrateIntoKeysFlow { predicate, action -> observeKeysPressed(predicate, action) }

        observeKeysPressed.invoke({ it.key == Key.I }) { isInfoOpen = !isInfoOpen }
    }

    MenuBar {

        Menu(text = "Actions") {
            Item(text = "Copy", onClick = pointsCopyPasteTarget::copy)
            Item(text = "Paste", onClick = pointsCopyPasteTarget::paste)
            Item(text = "Clear", onClick = points::clear)
        }

        Menu(text = "Assign") {
            worldAssignees.menuBarItems()
        }

        Menu(text = "Help") {
            Item(text = "components.Info", onClick = { isInfoOpen = true })
        }
    }

    MaterialTheme {
        Box(Modifier.fillMaxSize()) {
            ContextMenuArea(
                items = {
                    contextMenuAreaItems(
                        contextMenuSavedCursorOffset,
                        points.adjacencyMatrix,
                        selection.manuallySelected,
                        selection.selected,
                        createPointAction,
                        removeAction,
                        switchToDragModeAction,
                        splitInHalfAction,
                        connectAction,
                        disconnectAction,
                        clearSelectionAction
                    )
                },
                state = contextMenuState
            ) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .background(if (IS_TRANSPARENT_BUILD) Color(0x44ffffff) else Color.White)
                        .handleCursorInput(cursorInput)
                ) { // TODO move draw action into separate class, this will move part of parameters away from this function
                    world.drawCoordinateAxes()

                    for ((ai, bi) in points.connections) {
                        drawLine(Color.Black, canvasPoints[ai], canvasPoints[bi])
                    }

                    for (point in canvasPoints) {
                        drawCircle(Color.Black, 2F, point)
                    }

                    for (index in selection.selected) {
                        val isSelectionPosOffset = cursorInput.dragging && cursorInput.mode is CursorInput.Drag
                        val point = when (true) {
                            isSelectionPosOffset -> canvasPoints[index] + cursorInput.dragState.diff
                            else -> canvasPoints[index]
                        }
                        drawCircle(Color.Black, 4F, point, style = Stroke(1f))
                    }

                    if (!cursorInput.dragging) {
                        nearestPoint.drawPathToCursor()
                    }
                    cursorInput.draw()
                }
            }

            failures.console(Modifier.align(Alignment.BottomEnd).width(300.dp))
        }

        Info(isInfoOpen) { isInfoOpen = false }
        worldAssignees.dialogs()
    }
}

private fun contextMenuAreaItems(
    contextMenuSavedCursorOffset: Offset,
    adjacencyMatrix: List<List<Boolean>>,
    manuallySelectedPoints: List<Int>,
    affectedPointsIndices: List<Int>,
    createPointAction: (Offset) -> Unit,
    removeAction: () -> Unit,
    switchToDragModeAction: () -> Unit,
    splitInHalfAction: () -> Unit,
    connectAction: () -> Unit,
    disconnectAction: () -> Unit,
    clearSelectionAction: () -> Unit
) = buildList {
    add(ContextMenuItem("Create Point") { createPointAction(contextMenuSavedCursorOffset) })

    if (affectedPointsIndices.isNotEmpty()) {
        add(ContextMenuItem("Remove", removeAction))
        add(ContextMenuItem("Drag points", switchToDragModeAction))
    }
    if (
        affectedPointsIndices.size == 2 &&
        adjacencyMatrix[affectedPointsIndices[0]][affectedPointsIndices[1]]
    ) {
        add(ContextMenuItem("Split in half", splitInHalfAction))
    }
    if (affectedPointsIndices.size >= 2) {
        add(ContextMenuItem("Connect", connectAction))
        add(ContextMenuItem("Disconnect", disconnectAction))
    }
    if (manuallySelectedPoints.isNotEmpty()) {
        add(ContextMenuItem("Clear selection", clearSelectionAction))
    }
}

private operator fun Offset.times(other: Offset) = Offset(x * other.x, y * other.y)
private operator fun Offset.div(other: Offset) = Offset(x / other.x, y / other.y)