import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.onClick
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
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
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import common.OS
import common.currentOs
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import util.emitter
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

    var points by remember { mutableStateOf(listOf<Offset>()) }
    var connections by remember { mutableStateOf(listOf<Pair<Int, Int>>()) }

    var cursorOffset by remember { mutableStateOf<Offset?>(null) }
    var canvasSize by remember { mutableStateOf(Offset.Zero) }
    var magnetizing by remember { mutableStateOf(true) }
    var magneticPointIndex by remember { mutableStateOf<Int?>(null) }
    val magneticPoint by remember { derivedStateOf { magneticPointIndex?.let { points[it] } } }
    val nearestPoint by remember {
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
    val nearestPointIndex by remember { derivedStateOf { nearestPoint?.let { points.indexOf(nearestPoint) } } }

    if (!magnetizing) {
        magneticPointIndex = null
    }

    val copyAction = {
        val encoded = Json.encodeToString(SaveState(
            points.map { XY.fromOffset(it) },
            connections.flatMap { listOf(it.first, it.second) }
        ))
        clipboardManager.setText(AnnotatedString(encoded))
    }

    val pasteAction = {
        try {
            val loadedState = Json.decodeFromString<SaveState>(clipboardManager.getText()?.text ?: SaveState.EMPTY_JSON)

            if (loadedState.connections.any { it !in loadedState.points.indices }) {
                throw IllegalArgumentException("Bad indices in connections of pasted Json")
            }

            points = loadedState.points.map { it.toOffset() }
            connections = loadedState.connections.chunked(2) { it[0] to it[1] }
            magnetizing = false
            magneticPointIndex = null
        } catch (e: SerializationException) {
            e.printStackTrace()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            println(e.message) // TODO dialog
        }
    }

    val connectAction = connectAction@ {
        if (!magnetizing) return@connectAction
        nearestPointIndex?.let { connections += magneticPointIndex!! to it }
    }

    val toggleMagnetizingAction = {
        nearestPointIndex.takeUnless { magnetizing }?.let { i ->
            magnetizing = true
            magneticPointIndex = i
        } ?: run {
            magnetizing = false
        }
    }

    val consumePrimaryClick = { clickOffset: Offset ->
        magneticPointIndex.takeIf { magnetizing }?.let {
            connections += (it to points.size)
        }
        points += clickOffset
        magneticPointIndex = points.lastIndex
        magnetizing = true
    }

    val consumeCanvasSizeUpdate = { coordinates: LayoutCoordinates ->
        canvasSize = Offset(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
    }

    LaunchedEffect(Unit) {
        observeKeys.invoke(copyShortcutPredicate::test) { copyAction.invoke() }
        observeKeys.invoke(pasteShortcutPredicate::test) { pasteAction.invoke() }

        observeKeys.invoke({ it.key == Key.Spacebar }) { connectAction.invoke() }

        observeKeys.invoke({ it.key == Key.A }) { points = points.map { it.copy(x = it.x - 4) } }
        observeKeys.invoke({ it.key == Key.D }) { points = points.map { it.copy(x = it.x + 4) } }
        observeKeys.invoke({ it.key == Key.W }) { points = points.map { it.copy(y = it.y - 4) } }
        observeKeys.invoke({ it.key == Key.S }) { points = points.map { it.copy(y = it.y + 4) } }

        observeKeys.invoke({ it.key == Key.Z }) { points = points.map { it.copy(x = it.x * 0.99F) } }
        observeKeys.invoke({ it.key == Key.X }) { points = points.map { it.copy(x = it.x / 0.99F) } }
        observeKeys.invoke({ it.key == Key.C }) { points = points.map { it.copy(y = it.y * 0.99F) } }
        observeKeys.invoke({ it.key == Key.V }) { points = points.map { it.copy(y = it.y / 0.99F) } }
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
    }

    MaterialTheme {
        Canvas(Modifier
            .fillMaxSize()
            .background(if (IS_TRANSPARENT_BUILD) Color(0x44ffffff) else Color.White)
            .onPointerEvent(PointerEventType.Move) { cursorOffset = it.changes.first().position }
            .pointerInput(Unit) { detectTapGestures(onTap = consumePrimaryClick) }
            .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary), onClick = toggleMagnetizingAction)
            .onGloballyPositioned(consumeCanvasSizeUpdate)
        ) {
            for ((ai, bi) in connections) {
                drawLine(Color.Black, points[ai], points[bi])
            }

            for (point in points) {
                drawCircle(Color.Black, 4F, point)
            }

            cursorOffset?.let { cursorOffset ->
                magneticPoint?.let { magneticPoint ->
                    drawLine(Color.Black, cursorOffset, magneticPoint)
                }
                nearestPoint?.let { nearestPoint ->
                    drawLine(
                        Color.Black,
                        cursorOffset,
                        nearestPoint,
                        pathEffect = PathEffect.dashPathEffect(FloatArray(2) { 8F }, 0F)
                    )
                }
            }
        }
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
