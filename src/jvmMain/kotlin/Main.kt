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

    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    var points by remember { mutableStateOf(listOf<Offset>()) }
    var connections by remember { mutableStateOf(listOf<Pair<Int, Int>>()) }
    var cursorOffset by remember { mutableStateOf<Offset?>(null) }
    var canvasSize by remember { mutableStateOf(Offset.Zero) }

    var magnetizing by remember { mutableStateOf(true) }
    var magneticPointIndex by remember { mutableStateOf<Int?>(null) }
    val magneticPoint by remember { derivedStateOf { magneticPointIndex?.let { points[it] } } }

    val nearestPoint by remember { derivedStateOf { cursorOffset?.let { points.nearestPointTo(it) } } }
    val nearestPointIndex by remember { derivedStateOf { nearestPoint?.let { points.indexOf(nearestPoint) } } }

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
            points = loadedState.points.map { it.toOffset() }
            connections = loadedState.connections.chunked(2) { it[0] to it[1] }
            magnetizing = false
            magneticPointIndex = null
        } catch (e: SerializationException) {
            e.printStackTrace()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(Unit) {
        keysGlobalFlow
            .filter(copyShortcutPredicate::test)
            .distinctUntilChanged()
            .onEach { copyAction.invoke() }
            .launchIn(coroutineScope)

        keysGlobalFlow
            .filter(pasteShortcutPredicate::test)
            .distinctUntilChanged()
            .onEach { pasteAction.invoke() }
            .launchIn(coroutineScope)
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
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .onPointerEvent(PointerEventType.Move) {
                    cursorOffset = it.changes.first().position
                }
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        magneticPointIndex.takeIf { magnetizing }?.let {
                            connections += (it to points.size)
                        }
                        points += tapOffset
                        magneticPointIndex = points.lastIndex
                        magnetizing = true
                    }
                }
                .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary)) {
                    if (magnetizing) {
                        magnetizing = false
                    } else nearestPointIndex?.let { i ->
                        magnetizing = true
                        magneticPointIndex = i
                    }
                }
                .onGloballyPositioned {
                    canvasSize = Offset(it.size.width.toFloat(), it.size.height.toFloat())
                }
                .onKeyEvent {
                    println("key event happened")
                    return@onKeyEvent true
                }
        ) {
            connections
                .map { (ai, bi) -> points[ai] to points[bi] }
                .forEach { (a, b) -> drawLine(Color.Black, a, b) }

            for (point in points) {
                drawCircle(Color.Black, 4F, point)
            }

            run {
                val a = cursorOffset ?: return@run
                val b = (if (magnetizing) magneticPoint else nearestPoint) ?: return@run
                drawLine(Color.Black, a, b, pathEffect = PathEffect.dashPathEffect(FloatArray(2) { 8F }, 0F).takeUnless { magnetizing })
            }
        }
    }
}

private fun List<Offset>.nearestPointTo(destination: Offset) = minByOrNull { point -> (destination - point).getDistanceSquared() }

fun main() = application {
    val coroutineScope = rememberCoroutineScope()
    val keysFlow = remember { MutableSharedFlow<KeyEvent>() }

    Window(
        onCloseRequest = ::exitApplication,
        onKeyEvent = keysFlow.emitter(coroutineScope).returning(false)
    ) {
        App(keysFlow)
    }
}
