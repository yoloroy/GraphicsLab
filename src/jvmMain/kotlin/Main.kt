import androidx.compose.runtime.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import components.ComposableFailures
import components.CursorDragState
import components.CursorInput
import kotlinx.coroutines.flow.*
import util.createEmmitterIn
import util.partition
import util.returning
import java.lang.Math.toRadians

const val IS_TRANSPARENT_BUILD = false

fun main() = application {

    val coroutineScope = rememberCoroutineScope()

    // Suggestion: replace `keysFlow` with observers which will come from children composables
    // and will have type (KeyEvent) -> Boolean, which will allow usage of Boolean response in KeyEvent handling
    val keysFlow = remember { MutableSharedFlow<KeyEvent>() }
    var keysGlobalFlow: Flow<KeyEvent> = remember(keysFlow) { keysFlow }
    val cursor = remember { ComposableCursor() }
    var isShiftPressed by remember { mutableStateOf(false) }

    val observeKeys = { predicate: (KeyEvent) -> Boolean, action: (KeyEvent) -> Unit ->
        keysGlobalFlow
            .partition(predicate)
            .run { keysGlobalFlow = second; first }
            .distinctUntilChanged()
            .onEach(action)
            .launchIn(coroutineScope)
        Unit
    }

    val failures = remember { ComposableFailures() }
    val world = remember { ComposableWorld(failures) }
    val worldAssignees = rememberWorldAssignees(world)
    val worldInputTarget = remember { WorldInputTarget(world, 10f, 0.01f, toRadians(5.0).toFloat()) }
    val points = remember { ComposablePoints(failures) }
    val manualPointsSelection = remember { ComposablePointsSelection(points) }
    val canvasPoints = remember { CanvasPoints(points, world) }
    val nearestPoint by remember { mutableStateOf(ComposableNearestPoint(canvasPoints, cursor, manualPointsSelection, failures)) }
    val pointsSelection = remember { ComposablePointsSelectionAwareOfNearestPoint(nearestPoint, manualPointsSelection) }
    val cursorDragState = remember { CursorDragState() }
    val cursorInput = remember { CursorInput(cursor, pointsSelection, { isShiftPressed }, nearestPoint, canvasPoints, points, world, cursorDragState) }

    Window(
        onCloseRequest = ::exitApplication,
        onKeyEvent = keysFlow.createEmmitterIn(coroutineScope).returning(false),
        undecorated = IS_TRANSPARENT_BUILD,
        transparent = IS_TRANSPARENT_BUILD
    ) {
        val clipboardManager = LocalClipboardManager.current
        val pointsCopyPasteTarget = remember(clipboardManager) { PointsCopyPasteTarget(points, failures, clipboardManager) }

        // TODO refactor
        observeKeys({ it.key == Key.ShiftLeft || it.key == Key.ShiftRight }) { isShiftPressed = it.type == KeyEventType.KeyDown }

        App(
            failures,
            world,
            worldAssignees,
            worldInputTarget,
            points,
            pointsSelection,
            pointsCopyPasteTarget,
            keysFlow,
            observeKeys,
            cursorInput,
            nearestPoint,
            cursor
        )
    }
}
