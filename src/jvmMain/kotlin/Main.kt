import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.onClick
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import components.Failure
import components.FailuresLog
import components.ValueRetrieverDialog
import util.OS
import util.currentOs
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import util.emitter
import util.mapBoth
import util.returning
import java.util.function.Predicate

const val IS_TRANSPARENT_BUILD = false

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
    val points: List<XY>,
    val connections: List<Int>
) {
    companion object {
        const val EMPTY_JSON = "{ \"points\" = [], \"connections\" = [] }"
    }
}

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

    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    var failures by remember { mutableStateOf(listOf<Failure>()) }

    var points by remember { mutableStateOf(listOf<Offset>()) }
    var connections by remember { mutableStateOf(listOf<Pair<Int, Int>>()) }

    var worldOffset by remember { mutableStateOf(Offset(0F, 0F)) }
    var retrievingWorldOffset by remember { mutableStateOf(false) }

    var cursorOffset by remember { mutableStateOf<Offset?>(null) }
    var canvasSize by remember { mutableStateOf(Offset.Zero) }
    var magnetizing by remember { mutableStateOf(true) }
    var magneticPointIndex by remember { mutableStateOf<Int?>(null) }
    val magneticPoint by remember { derivedStateOf { magneticPointIndex?.let { points[it] } } }
    val nearestNotMagneticPoint by remember {
        derivedStateOf {
            cursorOffset?.let { // Suggestion: refactor `let`s
                magneticPoint?.let { magneticPoint ->
                    (points - magneticPoint).nearestPointTo(it)
                } ?: run {
                    points.nearestPointTo(it)
                }
            }
        }
    }
    val nearestNotMagneticPointIndex by remember { derivedStateOf { nearestNotMagneticPoint?.let { points.indexOf(nearestNotMagneticPoint) } } }

    if (!magnetizing) {
        magneticPointIndex = null
    }

    val copyAction = {
        try {
            val encoded = Json.encodeToString(SaveState(
                points.map { XY.fromOffset(it) },
                connections.flatMap { listOf(it.first, it.second) }
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

            points = loadedState.points.map { it.toOffset() }
            connections = loadedState.connections.chunked(2) { it[0] to it[1] }
            magnetizing = false
            magneticPointIndex = null
        } catch (e: SerializationException) {
            failures += Failure.Mistake("You did not pasted save")
        } catch (e: IllegalArgumentException) {
            failures += Failure.Mistake("Pasted save is not valid: ${e.message}")
        }
    }

    val connectAction = connectAction@ {
        if (!magnetizing) {
            failures += Failure.Mistake("You are not magnetizing to any point to make a connection")
            return@connectAction
        }
        nearestNotMagneticPointIndex?.let { connections += magneticPointIndex!! to it }
    }

    val removeAction = removeAction@ {
        if (magnetizing) {
            failures += Failure.Mistake("Toggle magnetizing off for enabling remove action")
            return@removeAction
        }

        cursorOffset?.let { cursorOffset ->
            val pointToRemove = points.nearestPointTo(cursorOffset) ?: run {
                failures += Failure.Mistake("There no points to remove")
                return@removeAction
            }
            val index = points.indexOf(pointToRemove)
            points -= pointToRemove
            connections = connections
                .filterNot { it.first == index || it.second == index }
                .mapBoth { if (it > index) it - 1 else it }
        }
    }

    val toggleMagnetizingAction = {
        nearestNotMagneticPointIndex.takeUnless { magnetizing }?.let { i ->
            magnetizing = true
            magneticPointIndex = i
        } ?: run {
            magnetizing = false
        }
    }

    val consumePrimaryClick = { clickOffset: Offset ->
        val inWorldOffset = clickOffset - worldOffset
        magneticPointIndex.takeIf { magnetizing }?.let {
            connections += (it to points.size)
        }
        points += inWorldOffset
        magneticPointIndex = points.lastIndex
        magnetizing = true
    }

    val consumeCanvasSizeUpdate = { coordinates: LayoutCoordinates ->
        canvasSize = Offset(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
    }

    val transformTextToPoint = { text: String ->
        try {
            text.split(" ")
                .map { it.toInt() }
                .let { Offset(it[0].toFloat(), it[1].toFloat()) }
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

        observeKeys.invoke({ it.key == Key.Spacebar }) { connectAction.invoke() }
        observeKeys.invoke({ it.key == Key.Backspace }) { removeAction.invoke() }

        observeKeys.invoke({ it.key == Key.A }) { worldOffset = worldOffset.copy(x = worldOffset.x - 4) }
        observeKeys.invoke({ it.key == Key.D }) { worldOffset = worldOffset.copy(x = worldOffset.x + 4) }
        observeKeys.invoke({ it.key == Key.W }) { worldOffset = worldOffset.copy(y = worldOffset.y - 4) }
        observeKeys.invoke({ it.key == Key.S }) { worldOffset = worldOffset.copy(y = worldOffset.y + 4) }

        observeKeys.invoke({ it.key == Key.R }) { points = points.map { it.copy(x = it.x * 0.99F) } } // TODO using worldZoom
        observeKeys.invoke({ it.key == Key.T }) { points = points.map { it.copy(x = it.x / 0.99F) } } // TODO using worldZoom
        observeKeys.invoke({ it.key == Key.F }) { points = points.map { it.copy(y = it.y * 0.99F) } } // TODO using worldZoom
        observeKeys.invoke({ it.key == Key.G }) { points = points.map { it.copy(y = it.y / 0.99F) } } // TODO using worldZoom
    }

    MenuBar {

        Menu(text = "Actions") {
            Item(text = "Copy", onClick = copyAction)
            Item(text = "Paste", onClick = pasteAction)
            Item(text = "Clear") {
                points = emptyList()
                connections = emptyList()
                magnetizing = false
                magneticPointIndex = null
            }
        }

        Menu(text = "Assign") {
            Item(text = "World offset", onClick = { retrievingWorldOffset = true })
        }
    }

    MaterialTheme {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier
                .fillMaxSize()
                .background(if (IS_TRANSPARENT_BUILD) Color(0x44ffffff) else Color.White)
                .onPointerEvent(PointerEventType.Move) { cursorOffset = it.changes.first().position }
                .pointerInput(Unit) { detectTapGestures(onTap = consumePrimaryClick) }
                .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary), onClick = toggleMagnetizingAction)
                .onGloballyPositioned(consumeCanvasSizeUpdate)
            ) {
                val offsetPoints = points
                    .map { it.copy(x = it.x + worldOffset.x, y = it.y + worldOffset.y) }

                for ((ai, bi) in connections) {
                    drawLine(Color.Black, offsetPoints[ai], offsetPoints[bi])
                }

                for (point in offsetPoints) {
                    drawCircle(Color.Black, 4F, point)
                }

                cursorOffset?.let { cursorOffset ->
                    magneticPointIndex?.let { i ->
                        drawLine(Color.Black, cursorOffset, offsetPoints[i])
                    }
                    nearestNotMagneticPointIndex?.let { i ->
                        drawLine(
                            Color.Black,
                            cursorOffset,
                            offsetPoints[i],
                            pathEffect = PathEffect.dashPathEffect(FloatArray(2) { 8F }, 0F)
                        )
                    }
                }
            }

            FailuresLog(failures, Modifier.align(Alignment.BottomEnd).width(300.dp))
        }

        // region dialogs
        ValueRetrieverDialog(
            startValue = worldOffset.let { "${it.x.toInt()} ${it.y.toInt()}" },
            visible = retrievingWorldOffset,
            title = "World Offset (two integer numbers separated by space)",
            transform = transformTextToPoint,
            setValueAndCloseDialog = {
                worldOffset = it
                retrievingWorldOffset = false
            },
            close = {
                retrievingWorldOffset = false
            }
        )
        // endregion
    }
}

private fun List<Offset>.nearestPointTo(destination: Offset) = minByOrNull { point -> (destination - point).getDistanceSquared() }

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
