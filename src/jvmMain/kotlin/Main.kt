import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.LayoutCoordinates
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
import util.*
import java.lang.Math.toDegrees
import java.lang.Math.toRadians
import java.util.function.Predicate
import kotlin.math.PI
import kotlin.math.pow
import kotlin.reflect.KProperty

const val IS_TRANSPARENT_BUILD = false
const val TWO_PI = 2 * PI

val copyShortcutPredicate: Predicate<KeyEvent> = run {
    when (currentOs) {
        OS.MAC -> Predicate { it.isMetaPressed && it.key == Key.C }
        else -> Predicate { it.isCtrlPressed && it.key == Key.C }
    }
}

val pasteShortcutPredicate: Predicate<KeyEvent> = run {
    when (currentOs) {
        OS.MAC -> Predicate { it.isMetaPressed && it.key == Key.V }
        else -> Predicate { it.isCtrlPressed && it.key == Key.V }
    }
}

@Serializable
data class SaveState(
    val points: List<XYZ>,
    val connections: List<Int>
) {
    companion object {
        const val EMPTY_JSON = "{ \"points\" = [], \"connections\" = [] }"
    }
}

enum class ScrollMode { Movement, Zoom, RotationXY }

context(FrameWindowScope)
@Composable
@Preview
fun App(keysGlobalFlow: Flow<KeyEvent>) {
    val coroutineScope = rememberCoroutineScope()
    val observeKeys = { predicate: (KeyEvent) -> Boolean, action: (KeyEvent) -> Unit ->
        keysGlobalFlow
            .filter { it.type == KeyEventType.KeyDown }
            .filter(predicate)
            .distinctUntilChanged()
            .onEach(action)
            .launchIn(coroutineScope)
    }

    // region ui
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var failures by remember { mutableStateOf(listOf<Failure>()) }
    var scrollMode by remember { mutableStateOf(ScrollMode.Movement) }
    var isInfoOpen by remember { mutableStateOf(false) }
    // endregion

    // region user utils
    var cursorOffset by remember { mutableStateOf<Offset?>(null) }
    var canvasSize by remember { mutableStateOf(Offset.Zero) }
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
    // endregion

    // region world
    var worldOffset by remember { mutableStateOf(XYZ.ZERO) }
    var retrievingWorldOffset by remember { mutableStateOf(false) }
    var worldScale by remember { mutableStateOf(XYZ.ONE) }
    var retrievingWorldScale by remember { mutableStateOf(false) }
    var worldXYRotation by remember { mutableStateOf(0F) }
    var worldYZRotation by remember { mutableStateOf(0F) }
    var worldZXRotation by remember { mutableStateOf(0F) }
    var retrievingWorldXYRotation by remember { mutableStateOf(false) }
    var retrievingWorldYZRotation by remember { mutableStateOf(false) }
    var retrievingWorldZXRotation by remember { mutableStateOf(false) }

    val canvasPoints by remember {
        derivedStateOf {
            points.map {
                (it scaled worldScale
                    `🔄Z` worldXYRotation
                    `🔄X` worldYZRotation
                    `🔄Y` worldZXRotation
                    offset worldOffset
                ).toOffset()
            }
        }
    }

    fun `🔄Z`(deltaRadians: Float) { worldXYRotation += deltaRadians }
    fun `🔄X`(deltaRadians: Float) { worldYZRotation += deltaRadians }
    fun `🔄Y`(deltaRadians: Float) { worldZXRotation += deltaRadians }

    LaunchedEffect(worldScale) {
        if (worldScale.x > 0F && worldScale.y > 0F) return@LaunchedEffect

        worldScale = XYZ(0.01F, 0.01F, 0.01F)
        failures += Failure.Mistake("World scale should be positive")
    }
    // endregion

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

    val onMove = { change: PointerInputChange ->
        cursorOffset = change.position
    }

    val onCanvasSizeUpdate = { coordinates: LayoutCoordinates ->
        canvasSize = Offset(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
    }

    val transformTextToXYZ = { text: String ->
        try {
            text.split(" ")
                .map { it.toFloat() }
                .let { XYZ(it[0], it[1], it[2]) }
        } catch (e: NumberFormatException) {
            null
        } catch (e: IndexOutOfBoundsException) {
            null
        } catch (e: Exception) {
            failures += Failure.UncaughtException(e.message ?: e::class.toString())
            worldOffset
        }
    }

    LaunchedEffect(Unit) {
        observeKeys.invoke(copyShortcutPredicate::test) { copyAction.invoke() }
        observeKeys.invoke(pasteShortcutPredicate::test) { pasteAction.invoke() }

        observeKeys.invoke({ it.key == Key.A }) { worldOffset = worldOffset.copy(x = worldOffset.x - 4) }
        observeKeys.invoke({ it.key == Key.D }) { worldOffset = worldOffset.copy(x = worldOffset.x + 4) }
        observeKeys.invoke({ it.key == Key.W }) { worldOffset = worldOffset.copy(y = worldOffset.y - 4) }
        observeKeys.invoke({ it.key == Key.S }) { worldOffset = worldOffset.copy(y = worldOffset.y + 4) }

        observeKeys.invoke({ it.key == Key.R }) { worldScale = worldScale.copy(x = worldScale.x * 0.99F) }
        observeKeys.invoke({ it.key == Key.T }) { worldScale = worldScale.copy(x = worldScale.x / 0.99F) }
        observeKeys.invoke({ it.key == Key.F }) { worldScale = worldScale.copy(y = worldScale.y * 0.99F) }
        observeKeys.invoke({ it.key == Key.G }) { worldScale = worldScale.copy(y = worldScale.y / 0.99F) }

        observeKeys.invoke({ it.key == Key.Q }) { `🔄Z`(-toRadians(5.0).toFloat()) }
        observeKeys.invoke({ it.key == Key.E }) { `🔄Z`(+toRadians(5.0).toFloat()) }
        observeKeys.invoke({ it.key == Key.Z }) { `🔄X`(-toRadians(5.0).toFloat()) }
        observeKeys.invoke({ it.key == Key.X }) { `🔄X`(+toRadians(5.0).toFloat()) }
        observeKeys.invoke({ it.key == Key.O }) { `🔄Y`(-toRadians(5.0).toFloat()) }
        observeKeys.invoke({ it.key == Key.L }) { `🔄Y`(+toRadians(5.0).toFloat()) }

        observeKeys.invoke({ it.key == Key.Y }) { scrollMode = ScrollMode.Movement }
        observeKeys.invoke({ it.key == Key.U }) { scrollMode = ScrollMode.Zoom }

        observeKeys.invoke({ it.key == Key.I }) { isInfoOpen = !isInfoOpen }
    }

    MenuBar {

        Menu(text = "Actions") {
            Item(text = "Copy", onClick = copyAction)
            Item(text = "Paste", onClick = pasteAction)
            Item(text = "Clear", onClick = clearAction)
        }

        Menu(text = "Assign") {
            Item(text = "World offset", onClick = { retrievingWorldOffset = true })
            Item(text = "World scale", onClick = { retrievingWorldScale = true })
            Item(text = "World XY rotation", onClick = { retrievingWorldXYRotation = true })
            Item(text = "World YZ rotation", onClick = { retrievingWorldYZRotation = true })
            Item(text = "World ZX rotation", onClick = { retrievingWorldZXRotation = true })
        }

        Menu(text = "Help") {
            Item(text = "Info", onClick = { isInfoOpen = true })
        }
    }

    MaterialTheme {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier
                .fillMaxSize()
                .background(if (IS_TRANSPARENT_BUILD) Color(0x44ffffff) else Color.White)
                .onCursorActions(
                    onMove = onMove, // onMove, // TODO moving on current plane
                    onScroll = {}, // onScroll, // TODO scale on current plane
                    onPrimaryClick = {}, // onPrimaryClick, // TODO
                    onToggleMagnetizingAction = {}, // onToggleMagnetizingAction, // TODO replace with context menu
                    onCanvasSizeUpdate = onCanvasSizeUpdate
                )
            ) {
                drawCoordinateAxes(worldOffset, worldXYRotation, worldYZRotation, worldZXRotation)

                for ((ai, bi) in connections) {
                    drawLine(Color.Black, canvasPoints[ai], canvasPoints[bi])
                }

                for (point in canvasPoints) {
                    drawCircle(Color.Black, 4F, point)
                }
            }

            FailuresLog(failures, Modifier.align(Alignment.BottomEnd).width(300.dp))
        }

        // region dialogs
        Info(isInfoOpen) { isInfoOpen = false }
        ValueRetrieverDialog(
            startValue = worldOffset.let { "${it.x} ${it.y} ${it.z}" },
            visible = retrievingWorldOffset,
            title = "World Offset (two integer numbers separated by space)",
            transform = transformTextToXYZ,
            setValueAndCloseDialog = {
                worldOffset = it
                retrievingWorldOffset = false
            },
            close = {
                retrievingWorldOffset = false
            }
        )
        ValueRetrieverDialog(
            startValue = worldScale.let { "${it.x} ${it.y} ${it.z}" },
            visible = retrievingWorldScale,
            title = "World Scale (three numbers separated by space)",
            transform = transformTextToXYZ,
            setValueAndCloseDialog = {
                worldScale = it
                retrievingWorldScale = false
            },
            close = {
                retrievingWorldScale = false
            }
        )
        ValueRetrieverDialog(
            startValue = toDegrees(worldXYRotation.toDouble() % TWO_PI).toString(),
            visible = retrievingWorldXYRotation,
            title = "World Rotation in XY (in degrees)",
            transform = { it.toDoubleOrNull()?.let { it % 360 } },
            setValueAndCloseDialog = {
                worldXYRotation = toRadians(it).toFloat()
                retrievingWorldXYRotation = false
            },
            close = {
                retrievingWorldXYRotation = false
            }
        )
        ValueRetrieverDialog(
            startValue = toDegrees(worldYZRotation.toDouble() % TWO_PI).toString(),
            visible = retrievingWorldYZRotation,
            title = "World Rotation in YZ (in degrees)",
            transform = { it.toDoubleOrNull()?.let { it % 360 } },
            setValueAndCloseDialog = {
                worldYZRotation = toRadians(it).toFloat()
                retrievingWorldYZRotation = false
            },
            close = {
                retrievingWorldYZRotation = false
            }
        )
        ValueRetrieverDialog(
            startValue = toDegrees(worldZXRotation.toDouble() % TWO_PI).toString(),
            visible = retrievingWorldZXRotation,
            title = "World Rotation in ZX (in degrees)",
            transform = { it.toDoubleOrNull()?.let { it % 360 } },
            setValueAndCloseDialog = {
                worldZXRotation = toRadians(it).toFloat()
                retrievingWorldZXRotation = false
            },
            close = {
                retrievingWorldZXRotation = false
            }
        )
        // endregion
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

private operator fun <T> Function0<T>.getValue(thisObj: Any?, property: KProperty<*>): T = invoke()

@Composable
private fun Info(visible: Boolean, close: () -> Unit) {
    if (!visible) return

    Dialog(
        visible = visible,
        title = "Info",
        onCloseRequest = close
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text("""
                Ctrl/⌘ + C – Copy figure
                Ctrl/⌘ + V – Paste figure
                ⌫ – Remove nearest point
                ⎵ – Connect magnetized point and other nearest point if they are not connected else disconnect
                W, A, S, D – move up, left, down, right accordingly
                Q, E – rotate left, rotate right
                R, T – scale width up and down
                F, G – scale height up and down
                Y – change scroll mode to movement
                U – change scroll mode to zooming
                H – change scroll mode to xy rotation
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
