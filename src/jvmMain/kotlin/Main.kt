import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
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
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import components.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import state_holders.rememberWorldAssignees
import state_holders.rememberWorld
import util.*
import java.lang.Math.toRadians
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

const val IS_TRANSPARENT_BUILD = false

@Serializable
data class SaveState(
    val points: List<XYZ>,
    val connections: List<Int>
) {
    companion object {
        const val EMPTY_JSON = "{ \"points\" = [], \"connections\" = [] }"
    }
}

enum class DragMode {
    Selection, Drag
}

context(FrameWindowScope)
@Composable
@Preview
fun App(keysGlobalFlow: Flow<KeyEvent>) {
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
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var failures by remember { mutableStateOf(listOf<Failure>()) }
    var isInfoOpen by remember { mutableStateOf(false) }
    // endregion

    // region user utils
    var isShiftPressed by remember { mutableStateOf(false) }
    // endregion

    // region points
    var points by remember { mutableStateOf(listOf<XYZ>()) }
    var adjacencyMatrix by remember { mutableStateOf(mutableListOf<MutableList<Boolean>>(), neverEqualPolicy()) }
    val connections by { adjacencyMatrix
        .asSequence()
        .flatMapIndexed { ai, connections -> connections
            .asSequence()
            .withIndex()
            .filter { it.value }
            .map { (bi, _) -> ai to bi }
        }
    }

    fun connect(ai: Int, bi: Int) {
        adjacencyMatrix = adjacencyMatrix.apply {
            this[ai][bi] = true
            this[bi][ai] = true
        }
    }

    fun disconnect(ai: Int, bi: Int) {
        adjacencyMatrix = adjacencyMatrix.apply {
            this[ai][bi] = false
            this[bi][ai] = false
        }
    }

    fun toggleConnection(ai: Int, bi: Int) = if (adjacencyMatrix[ai][bi]) disconnect(ai, bi) else connect(ai, bi)

    fun addPoint(xyz: XYZ): Int {
        points += xyz
        adjacencyMatrix = adjacencyMatrix.apply {
            forEach { it += false }
            this += MutableList(points.size) { false }
        }
        return points.lastIndex
    }

    fun removePointAt(index: Int) {
        points = points.dropAt(index)
        adjacencyMatrix = adjacencyMatrix.apply {
            removeAt(index)
            forEach { it.removeAt(index) }
        }
    }
    // endregion

    // region world
    val world = rememberWorld()
    val worldAssignees = rememberWorldAssignees(world)

    fun XYZ.toCanvas() = scaled(world.scale).`🔄Z`(world.xyRadians).`🔄X`(world.yzRadians).`🔄Y`(world.zxRadians).offset(world.offset).toOffset()

    val canvasPoints by remember { derivedStateOf { points.map { it.toCanvas() } } }

    fun `🔄Z`(deltaRadians: Float) { world.xyRadians += deltaRadians }
    fun `🔄X`(deltaRadians: Float) { world.yzRadians += deltaRadians }
    fun `🔄Y`(deltaRadians: Float) { world.zxRadians += deltaRadians }

    fun Offset.toWorldXYZ() = toWorldXYZ(world.offset, world.scale, world.xyRadians, world.yzRadians, world.zxRadians)

    LaunchedEffect(world.scale) {
        if (world.scale.x > 0F && world.scale.y > 0F) return@LaunchedEffect

        world.scale = XYZ(0.01F, 0.01F, 0.01F)
        failures += Failure.Mistake("World scale should be positive")
    }
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

    val copyAction = {
        try {
            val encoded = Json.encodeToString(SaveState(
                points,
                connections.flatMap { listOf(it.first, it.second) }.toList()
            ))
            clipboardManager.setText(AnnotatedString(encoded))
        } catch (e: Exception) {
            failures += Failure.UncaughtException(e.message ?: "Uncaught exception")
        }
    }

    val pasteAction = {
        try {
            val loadedState = Json.decodeFromString<SaveState>(clipboardManager.getText()?.text ?: SaveState.EMPTY_JSON)

            if (loadedState.connections.any { it !in loadedState.points.indices }) {
                throw IllegalStateException("Bad indices in connections of pasted Json")
            }

            points = loadedState.points
            adjacencyMatrix = MutableList(points.size) { MutableList(points.size) { false } }.apply {
                loadedState.connections.chunked(2).forEach { (ai, bi) ->
                    this[ai][bi] = true
                    this[bi][ai] = true
                }
            }
        } catch (e: SerializationException) {
            failures += Failure.Mistake("You did not pasted save")
        } catch (e: IllegalArgumentException) {
            failures += Failure.Mistake("Pasted save is not valid: ${e.message}")
        }
    }

    val clearAction = {
        points = emptyList()
        adjacencyMatrix = mutableListOf()
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
            failures += Failure.Mistake("Nearest point does not exists")
        }
    }

    val selectAllAction = {
        manuallySelectedPoints = points.indices.toList()
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
        val (a, b) = affectedPointsIndices.map { points[it] }

        disconnect(ai, bi)
        addPoint(listOf(a, b).average()).let { i ->
            connect(ai, i)
            connect(bi, i)
        }
    }

    val createPointAction = { savedCursorOffset: Offset ->
        switchToSelectionModeAction()
        addPoint(savedCursorOffset.toWorldXYZ())
    }

    val connectAction = deselectionContext {
        for ((ai, bi) in affectedPointsIndices.combinationsOfPairs()) {
            connect(ai, bi)
        }
    }

    val disconnectAction = deselectionContext {
        for ((ai, bi) in affectedPointsIndices.combinationsOfPairs()) {
            disconnect(ai, bi)
        }
    }

    val toggleConnectionAction = {
        for ((ai, bi) in affectedPointsIndices.combinationsOfPairs()) {
            toggleConnection(ai, bi)
        }
    }

    val removeAction = deselectionContext {
        for ((i, iToRemove) in affectedPointsIndices.withIndex()) {
            val removingOffset = -affectedPointsIndices.asSequence().take(i).count { it < iToRemove }
            removePointAt(iToRemove + removingOffset)
        }

        manuallySelectedPoints = emptyList()
    }

    val assignWorldRotation = { xy: Number, yz: Number, zx: Number ->
        world.xyRadians = xy.toFloat()
        world.yzRadians = yz.toFloat()
        world.zxRadians = zx.toFloat()
    }

    val dragPointsAction = { start: Offset, end: Offset ->
        val xyzOffset = end.toWorldXYZ() - start.toWorldXYZ()
        points = points.mapIndexed { i, point ->
            if (i in affectedPointsIndices) {
                point + xyzOffset
            } else {
                point
            }
        }
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
                failures += Failure.Mistake("Nearest point does not exists")
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
        observeKeysPressed.invoke({ it.isWinCtrlPressed && it.key == Key.C }) { copyAction.invoke() }
        observeKeysPressed.invoke({ it.isWinCtrlPressed && it.key == Key.V }) { pasteAction.invoke() }

        observeKeys.invoke({ it.key in listOf(Key.ShiftLeft, Key.ShiftRight) }) { isShiftPressed = it.type == KeyEventType.KeyDown }
        observeKeysPressed.invoke({ it.key == Key.Backspace }) { removeAction.invoke() }

        observeKeysPressed.invoke({ it.isWinCtrlPressed && it.key == Key.A }) { selectAllAction.invoke() }
        observeKeysPressed.invoke({ it.key == Key.Spacebar }) { toggleConnectionAction.invoke() }

        // region world config
        observeKeysPressed.invoke({ it.key == Key.One }) { assignWorldRotation.invoke(0, 0, 0) }
        observeKeysPressed.invoke({ it.key == Key.Two }) { assignWorldRotation.invoke(0, 0, PI / 2) }
        observeKeysPressed.invoke({ it.key == Key.Three }) { assignWorldRotation.invoke(0, PI / 2, 0) }
        observeKeysPressed.invoke({ it.key == Key.Four }) { assignWorldRotation.invoke(0, PI / 4, PI / 4) }

        observeKeysPressed.invoke({ it.key == Key.W }) { world.offset = world.offset.copy(y = world.offset.y - 4) }
        observeKeysPressed.invoke({ it.key == Key.A }) { world.offset = world.offset.copy(x = world.offset.x - 4) }
        observeKeysPressed.invoke({ it.key == Key.S }) { world.offset = world.offset.copy(y = world.offset.y + 4) }
        observeKeysPressed.invoke({ it.key == Key.D }) { world.offset = world.offset.copy(x = world.offset.x + 4) }
        observeKeysPressed.invoke({ it.key == Key.DirectionUp }) { world.offset = world.offset.copy(z = world.offset.z + 4) }
        observeKeysPressed.invoke({ it.key == Key.DirectionDown }) { world.offset = world.offset.copy(z = world.offset.z - 4) }

        observeKeysPressed.invoke({ it.key == Key.R }) { world.scale = world.scale.copy(x = world.scale.x * 0.99F) }
        observeKeysPressed.invoke({ it.key == Key.T }) { world.scale = world.scale.copy(x = world.scale.x / 0.99F) }
        observeKeysPressed.invoke({ it.key == Key.F }) { world.scale = world.scale.copy(y = world.scale.y * 0.99F) }
        observeKeysPressed.invoke({ it.key == Key.G }) { world.scale = world.scale.copy(y = world.scale.y / 0.99F) }

        observeKeysPressed.invoke({ it.key == Key.Q }) { `🔄Z`(-toRadians(5.0).toFloat()) }
        observeKeysPressed.invoke({ it.key == Key.E }) { `🔄Z`(+toRadians(5.0).toFloat()) }
        observeKeysPressed.invoke({ it.key == Key.Z }) { `🔄X`(-toRadians(5.0).toFloat()) }
        observeKeysPressed.invoke({ it.key == Key.X }) { `🔄X`(+toRadians(5.0).toFloat()) }
        observeKeysPressed.invoke({ it.key == Key.C }) { `🔄Y`(-toRadians(5.0).toFloat()) }
        observeKeysPressed.invoke({ it.key == Key.V }) { `🔄Y`(+toRadians(5.0).toFloat()) }
        // endregion

        observeKeysPressed.invoke({ it.key == Key.I }) { isInfoOpen = !isInfoOpen }
    }

    MenuBar {

        Menu(text = "Actions") {
            Item(text = "Copy", onClick = copyAction)
            Item(text = "Paste", onClick = pasteAction)
            Item(text = "Clear", onClick = clearAction)
        }

        Menu(text = "Assign") {
            worldAssignees.menuBarItems()
        }

        Menu(text = "Help") {
            Item(text = "Info", onClick = { isInfoOpen = true })
        }
    }

    MaterialTheme {
        Box(Modifier.fillMaxSize()) {
            ContextMenuArea(
                items = {
                    contextMenuAreaItems(
                        contextMenuSavedCursorOffset,
                        nearestPointIndex,
                        adjacencyMatrix,
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

                    for ((ai, bi) in connections) {
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
                            val center = affectedPointsIndices.map { points[it] }.average()
                            drawDragOffset(center, center + cursorDragState.xyzDiff(), XYZ::toCanvas)
                        }
                    }
                }
            }

            FailuresLog(failures, Modifier.align(Alignment.BottomEnd).width(300.dp))
        }

        Info(isInfoOpen) { isInfoOpen = false }
        worldAssignees.dialogs()
    }
}

private fun contextMenuAreaItems(
    contextMenuSavedCursorOffset: Offset,
    nearestPointIndex: Int?,
    adjacencyMatrix: MutableList<MutableList<Boolean>>,
    manuallySelectedPoints: List<Int>,
    affectedPointsIndices: List<Int>,
    createPointAction: (Offset) -> Int,
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

@Composable
private fun Info(visible: Boolean, close: () -> Unit) {
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
            TextButton(onClick = close) {
                Text("OK")
            }
        }
    }
}

private operator fun Offset.times(other: Offset) = Offset(x * other.x, y * other.y)

private operator fun Offset.div(other: Offset) = Offset(x / other.x, y / other.y)

fun main() = application {
    val coroutineScope = rememberCoroutineScope()
    // Suggestion: replace `keysFlow` with observers which will come from children composables
    // and will have type (KeyEvent) -> Boolean, which will allow usage of Boolean response in KeyEvent handling
    val keysFlow = remember { MutableSharedFlow<KeyEvent>() }

    Window(
        onCloseRequest = ::exitApplication,
        onKeyEvent = keysFlow.emitter(coroutineScope).returning(false),
        undecorated = IS_TRANSPARENT_BUILD,
        transparent = IS_TRANSPARENT_BUILD
    ) {
        App(keysFlow)
    }
}
