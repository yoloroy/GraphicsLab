import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import components.CanvasContextMenu
import components.Failures
import components.Info

context(FrameWindowScope)
@Composable
@Preview
fun App(
    failures: Failures,
    worldAssignees: WorldAssignees,
    pointsGeneralActions: PointsGeneralActions,
    info: Info,
    canvasContextMenu: CanvasContextMenu,
    pointsCanvas: PointsCanvas,
    renderMode: ComposableRenderMode
) {
    MenuBar {
        Menu(text = "Actions") {
            pointsGeneralActions.MenuBarItems()
        }
        Menu(text = "Assign") {
            worldAssignees.MenuBarItems()
        }
        Menu(text = "Render") {
            renderMode.MenuBarItems()
        }
        Menu(text = "Help") {
            info.MenuBarItem()
        }
    }

    MaterialTheme {
        Box(Modifier.fillMaxSize()) {
            canvasContextMenu.Area {
                pointsCanvas.View(Modifier.fillMaxSize())
            }
            failures.Console(Modifier.align(Alignment.BottomEnd).width(300.dp))
        }
        info.Dialog()
        worldAssignees.Dialogs()
        renderMode.Dialogs()
    }
}
