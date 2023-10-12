import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.MenuScope
import components.Assignee
import components.Failures
import util.forIntSize

interface RenderMode {

    context(MenuScope)
    @Composable
    fun MenuBarItems()

    @Composable
    fun Dialogs()
}

class ComposableRenderMode(
    private val pointsCanvas: ComposablePointsCanvas, // TODO refactor direct usage
    private val failures: Failures,
    startWireframeComponent: WireframeComponent,
    startRayTracingComponent: RayTracingComponent,
    startScreenSize: IntSize
): RenderMode {

    private val wireframeComponent = startWireframeComponent
    private var rayTracingComponent = startRayTracingComponent.apply { isPlaying = false }

    private val renderingSizeAssignee = Assignee.forIntSize(
        "Rendering screen size",
        ::updateRayTracingScreenSize,
        startScreenSize.run { "$width $height" }
    )

    context(MenuScope)
    @Composable
    override fun MenuBarItems() {
        Item(text = "Wireframe mode") {
            rayTracingComponent.isPlaying = false
            pointsCanvas.trianglesComponent = wireframeComponent
        }
        Item(text = "RayTracing mode") {
            pointsCanvas.trianglesComponent = rayTracingComponent
            rayTracingComponent.isPlaying = true
        }
        if (pointsCanvas.trianglesComponent is RayTracingComponent) {
            Item(text = "Assign rendering screen size") {
                rayTracingComponent.isPlaying = false
                renderingSizeAssignee.open()
            }
        }
    }

    @Composable
    override fun Dialogs() {
        renderingSizeAssignee.dialog()
    }

    private fun updateRayTracingScreenSize(size: IntSize) {
        rayTracingComponent.isPlaying = false
        rayTracingComponent = rayTracingComponent.copyWithOtherScreenSize(size)

        if (pointsCanvas.trianglesComponent is RayTracingComponent) {
            pointsCanvas.trianglesComponent = rayTracingComponent
            rayTracingComponent.isPlaying = true
        } else {
            failures.logException("Triangles component of ComposablePointsCanvas !is RayTracingComponent")
        }
    }
}
