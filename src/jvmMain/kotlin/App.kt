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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import components.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import util.*
import kotlin.math.max
import kotlin.math.min

private enum class DragMode {
    Selection, Drag
}

context(FrameWindowScope)
@Composable
@Preview
fun App(
    failures: Failures,
    world: World,
    worldAssignees: WorldAssignees,
    worldInputTarget: WorldInputTarget,
    points: Points,
    pointsCopyPasteTarget: PointsCopyPasteTarget,
    keysGlobalFlow: Flow<KeyEvent>
) {
    val coroutineScope = rememberCoroutineScope()

    var keysFlow by remember(keysGlobalFlow) { mutableStateOf(keysGlobalFlow) }
    val observeKeysPressed = { predicate: (KeyEvent) -> Boolean, action: (KeyEvent) -> Unit ->
        keysFlow
            .partition { it.type == KeyEventType.KeyDown && predicate(it) }
            .run { keysFlow = second; first }
            .distinctUntilChanged()
            .onEach(action)
            .launchIn(coroutineScope)
    }
    val observeKeys = { predicate: (KeyEvent) -> Boolean, action: (KeyEvent) -> Unit ->
        keysFlow
            .partition(predicate)
            .run { keysFlow = second; first }
            .distinctUntilChanged()
            .onEach(action)
            .launchIn(coroutineScope)
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
    var cursorOffset by remember { mutableStateOf(Offset.Zero) }
    var dragMode by remember { mutableStateOf(DragMode.Selection) }
    val cursorDragState = rememberCursorDragState(cursorOffset)

    fun CursorDragState.xyzDiff() = end.toWorldXYZ() - start.toWorldXYZ()

    val nearestPointIndex by remember {
        derivedStateOf {
            canvasPoints.takeIfNotEmpty()?.indexOfMinBy { it.distanceTo(cursorOffset) }
        }
    }
    var manuallySelectedPoints by remember { mutableStateOf(listOf<Int>()) }
    val affectedPointsIndices by remember {
        derivedStateOf {
            manuallySelectedPoints.takeIfNotEmpty()
                ?: nearestPointIndex?.let { listOf(it) }
                ?: emptyList()
        }
    }

    val contextMenuState = remember { ContextMenuState() }
    val contextMenuSavedCursorOffset = remember(contextMenuState.status) { cursorOffset }
    // endregion

    val switchToDragModeAction = {
        dragMode = DragMode.Drag
    }

    val switchToSelectionModeAction = {
        dragMode = DragMode.Selection
    }

    fun deselectionContext(block: () -> Unit) = ({
        switchToSelectionModeAction()
        block()
        manuallySelectedPoints = emptyList()
    })

    val selectSinglePointAction = {
        switchToSelectionModeAction()
        nearestPointIndex?.let {
            manuallySelectedPoints = when {
                isShiftPressed -> when (it) {
                    in manuallySelectedPoints -> manuallySelectedPoints - it
                    else -> manuallySelectedPoints + it
                }
                listOf(it) == manuallySelectedPoints -> emptyList()
                else -> listOf(it)
            }
        } ?: run {
            failures.logMistake("Nearest point does not exists")
        }
    }

    val selectAllAction = {
        manuallySelectedPoints = points.points.indices.toList()
    }

    val selectAreaAction = { rect: Rect ->
        manuallySelectedPoints = canvasPoints
            .withIndex()
            .filter { it.value in rect }
            .retrieveIndices()
            .plus(if (isShiftPressed) manuallySelectedPoints else emptyList())
    }

    val clearSelectionAction = {
        switchToSelectionModeAction()
        manuallySelectedPoints = emptyList()
    }

    val splitInHalfAction = {
        switchToSelectionModeAction()
        val (ai, bi) = affectedPointsIndices
        points.splitInHalf(ai, bi)
    }

    val createPointAction = { savedCursorOffset: Offset ->
        switchToSelectionModeAction()
        points.append(savedCursorOffset.toWorldXYZ())
    }

    val connectAction = deselectionContext {
        points.connectAll(affectedPointsIndices)
    }

    val disconnectAction = deselectionContext {
        points.disconnectAll(affectedPointsIndices)
    }

    val toggleConnectionAction = {
        points.toggleConnections(affectedPointsIndices)
    }

    val removeAction = deselectionContext {
        points.removeAll(affectedPointsIndices)
    }

    val dragPointsAction = { start: Offset, end: Offset ->
        val xyzOffset = end.toWorldXYZ() - start.toWorldXYZ()
        points.transform(affectedPointsIndices) { it + xyzOffset }
    }

    val onMove = { change: PointerInputChange ->
        cursorOffset = change.position
    }

    val onPrimaryClick = onPrimaryClick@ {
        switchToSelectionModeAction()
        if (isShiftPressed) {
            nearestPointIndex?.let {
                manuallySelectedPoints += it
            } ?: run {
                failures.logMistake("Nearest point does not exists")
            }
            return@onPrimaryClick
        }
        selectSinglePointAction()
    }

    cursorDragState.observeOnDragEnd { start: Offset, end: Offset ->
        when (dragMode) {
            DragMode.Selection -> selectAreaAction(Rect.areaOf(start, end))
            DragMode.Drag -> dragPointsAction(start, end)
        }
    }

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
                        nearestPointIndex,
                        points.adjacencyMatrix,
                        manuallySelectedPoints,
                        affectedPointsIndices,
                        createPointAction,
                        removeAction,
                        switchToDragModeAction,
                        selectSinglePointAction,
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
                        .onCursorActions(cursorDragState, onMove, onPrimaryClick)
                ) {
                    drawCoordinateAxes(world.offset, world.xyRadians, world.yzRadians, world.zxRadians)

                    for ((ai, bi) in points.connections) {
                        drawLine(Color.Black, canvasPoints[ai], canvasPoints[bi])
                    }

                    for (point in canvasPoints) {
                        drawCircle(Color.Black, 2F, point)
                    }

                    for (index in affectedPointsIndices) {
                        val point = when (true) {
                            (cursorDragState.dragging && dragMode == DragMode.Drag) -> canvasPoints[index] + cursorDragState.diff
                            else -> canvasPoints[index]
                        }
                        drawCircle(Color.Black, 4F, point, style = Stroke(1f))
                    }

                    nearestPointIndex.takeUnless { cursorDragState.dragging }?.let { nearestPointIndex ->
                        drawLine(
                            color = Color.Black,
                            start = cursorOffset,
                            end = canvasPoints[nearestPointIndex],
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )
                    }

                    if (cursorDragState.dragging) when (dragMode) {
                        DragMode.Selection -> drawSelectionArea(cursorDragState.start, cursorDragState.end)
                        DragMode.Drag -> {
                            val center = affectedPointsIndices.map { points.points[it] }.average()
                            drawDragOffset(center, center + cursorDragState.xyzDiff(), XYZ::toCanvas)
                        }
                    }
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
    nearestPointIndex: Int?,
    adjacencyMatrix: List<List<Boolean>>,
    manuallySelectedPoints: List<Int>,
    affectedPointsIndices: List<Int>,
    createPointAction: (Offset) -> Unit,
    removeAction: () -> Unit,
    switchToDragModeAction: () -> Unit,
    selectSinglePointAction: () -> Unit,
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
    if (nearestPointIndex != null) {
        add(ContextMenuItem("Select", selectSinglePointAction))
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

private fun DrawScope.drawCoordinateAxes(offset: XYZ, xYRotation: Float, yZRotation: Float, zXRotation: Float) {
    val points = listOf(
        XYZ(-1f, 0f, 0f), XYZ(1f, 0f, 0f), XYZ(0f, -1f, 0f), XYZ(0f, 1f, 0f), XYZ(0f, 0f, -1f), XYZ(0f, 0f, 1f)
    ).map {
        (it
            `🔄Z` xYRotation
            `🔄X` yZRotation
            `🔄Y` zXRotation
            scaled XYZ(size.width, size.height)
            scaled XYZ(10F, 10F)
            offset offset
        ).toOffset()
    }

    drawLine(Color.Red, points[0], points[1])
    drawLine(Color.Blue, points[2], points[3])
    drawLine(Color.Green, points[4], points[5])
}

private fun DrawScope.drawSelectionArea(start: Offset, end: Offset) {
    val topLeft = Offset(min(start.x, end.x), min(start.y, end.y))
    val bottomRight = Offset(max(start.x, end.x), max(start.y, end.y))
    val size = (bottomRight - topLeft).run { Size(x, y) }

    drawCircle(Color.Black, 4f, start, 0.4f)
    drawCircle(Color.Black, 4f, end, 0.4f)

    drawRect(Color.Black, topLeft, size, 0.4f, Stroke(1f))
}

private fun DrawScope.drawDragOffset(start: XYZ, end: XYZ, convertToOffset: XYZ.() -> Offset) {
    val diff = end - start

    val startOffset = start.convertToOffset()
    val endOffset = end.convertToOffset()
    val xShiftEnd = (start + XYZ.ZERO.copy(x = diff.x)).convertToOffset()
    val yShiftEnd = (start + XYZ.ZERO.copy(y = diff.y)).convertToOffset()
    val zShiftEnd = (start + XYZ.ZERO.copy(z = diff.z)).convertToOffset()

    drawArrow(Color.Black, startOffset, endOffset)
    drawArrow(Color.Red, startOffset, xShiftEnd)
    drawArrow(Color.Blue, startOffset, yShiftEnd)
    drawArrow(Color.Green, startOffset, zShiftEnd)
}

private operator fun Offset.times(other: Offset) = Offset(x * other.x, y * other.y)
private operator fun Offset.div(other: Offset) = Offset(x / other.x, y / other.y)