import androidx.compose.runtime.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import components.*
import kotlinx.coroutines.flow.*
import util.*
import java.lang.Math.toRadians

const val IS_TRANSPARENT_BUILD = false

fun main() = application {

    val coroutineScope = rememberCoroutineScope()

    // Suggestion: replace `keysFlow` with observers which will come from children composables
    // and will have type (KeyEvent) -> Boolean, which will allow usage of Boolean response in KeyEvent handling
    val keysFlow = remember { MutableSharedFlow<KeyEvent>() }

    val failures = remember { ComposableFailures() }
    val world = remember { ComposableWorld(failures) }
    val worldAssignees = rememberWorldAssignees(world)
    val worldInputTarget = remember { WorldInputTarget(world, 10f, 0.01f, toRadians(5.0).toFloat()) }
    val points = remember { ComposablePoints(failures) }

    Window(
        onCloseRequest = ::exitApplication,
        onKeyEvent = keysFlow.createEmmitterIn(coroutineScope).returning(false),
        undecorated = IS_TRANSPARENT_BUILD,
        transparent = IS_TRANSPARENT_BUILD
    ) {
        val clipboardManager = LocalClipboardManager.current
        val pointsCopyPasteTarget = remember(clipboardManager) { PointsCopyPasteTarget(points, failures, clipboardManager) }

        App(failures, world, worldAssignees, worldInputTarget, points, pointsCopyPasteTarget, keysFlow)
    }
}
