import androidx.compose.runtime.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import components.*
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
    val observeKeysPressed = { predicate: (KeyEvent) -> Boolean, action: (KeyEvent) -> Unit ->
        observeKeys({ it.type == KeyEventType.KeyDown && predicate(it) }, action)
    }

    val failures = remember { ComposableFailures() }
    val world = remember { ComposableWorld(failures) }
    val worldAssignees = rememberWorldAssignees(world)
    val worldInputTarget = remember { WorldInputTargetImpl(world, 10f, 0.01f, toRadians(5.0).toFloat()) }
    val points = remember { ComposablePoints(failures) }
    val manualPointsSelection = remember { ComposablePointsSelection(points) }
    val canvasPoints = remember { ComposableCanvasPoints(points, world) }
    val nearestPoint by remember { mutableStateOf(ComposableNearestPoint(canvasPoints, cursor, manualPointsSelection, failures)) }
    val pointsSelection = remember { ComposablePointsSelectionAwareOfNearestPoint(nearestPoint, manualPointsSelection) }
    val cursorDragState = remember { CursorDragState() }
    val cursorInput = remember { CursorInput(cursor, pointsSelection, { isShiftPressed }, nearestPoint, canvasPoints, points, world, cursorDragState) }

    val cursorXYZ = remember { ComposableCursorXYZ(cursor, world) }
    val fullPointsSelection by remember {
        derivedStateOf {
            PointsSelectionFeaturingSwitchingCursorInputModeToSelection(
                PointsSelectionFeaturingDeselection(pointsSelection),
                cursorInput
            )
        }
    }
    val wireframeComponent = WireframeComponent(canvasPoints, world)
    val rayTracingComponent = RayTracingComponent(canvasPoints, IntSize(800, 550), world, rememberCoroutineScope())
    val pointsCanvas = remember {
        ComposablePointsCanvas(
            isTransparentBuild = IS_TRANSPARENT_BUILD,
            cursorInput = cursorInput,
            points = canvasPoints,
            selection = fullPointsSelection,
            nearestPoint = nearestPoint,
            trianglesComponent = wireframeComponent
        )
    }
    val renderMode = remember { ComposableRenderMode(pointsCanvas, failures, wireframeComponent, rayTracingComponent, IntSize(800, 550)) }
    val canvasContextMenu = remember { ComposableCanvasContextMenu(fullPointsSelection, cursorInput, cursorXYZ, points) }
    val info = remember { ComposableInfo() }

    Window(
        onCloseRequest = ::exitApplication,
        onKeyEvent = keysFlow.createEmmitterIn(coroutineScope).returning(false),
        undecorated = IS_TRANSPARENT_BUILD,
        transparent = IS_TRANSPARENT_BUILD
    ) {
        val clipboardManager = LocalClipboardManager.current
        val pointsGeneralActions = remember(clipboardManager) { PointsGeneralActions(points, failures, clipboardManager, fullPointsSelection) }

        LaunchedEffect(Unit) {
            observeKeys({ it.key == Key.ShiftLeft || it.key == Key.ShiftRight }) {
                isShiftPressed = it.type == KeyEventType.KeyDown
            }
            pointsGeneralActions.integrateIntoKeysFlow(observeKeysPressed)
            fullPointsSelection.integrateIntoKeysFlow(observeKeysPressed)
            worldInputTarget.integrateIntoKeysFlow(observeKeysPressed)
            info.integrateIntoKeysFlow(observeKeysPressed)
        }

        App(
            failures,
            worldAssignees,
            pointsGeneralActions,
            info,
            canvasContextMenu,
            pointsCanvas,
            renderMode
        )
    }
}
