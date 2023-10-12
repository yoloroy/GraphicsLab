import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.window.MenuScope
import components.Assignee
import components.Failures
import util.TWO_PI
import util.forFloat
import util.forXYZ
import java.lang.Math.toDegrees

interface World {
    val offset: XYZ
    val scale: XYZ
    val xyRadians: Float
    val yzRadians: Float
    val zxRadians: Float

    context(DrawScope)
    fun drawCoordinateAxes()
}

class ComposableWorld(private val failures: Failures): World {

    private var scaleState by mutableStateOf(XYZ.ONE)

    override var offset by mutableStateOf(XYZ.ZERO)

    override var scale
        get() = scaleState
        set(value) {
            scaleState = if (value.x > 0F && value.y > 0F && value.z > 0F) {
                value
            } else {
                failures.logMistake("World scale should be > 0")
                XYZ(0.01F, 0.01F, 0.01F)
            }
        }

    override var xyRadians by mutableStateOf(0F)

    override var yzRadians by mutableStateOf(0F)

    override var zxRadians by mutableStateOf(0F)

    context(DrawScope)
    override fun drawCoordinateAxes() {
        val points = listOf(
            XYZ(-1f, 0f, 0f), XYZ(1f, 0f, 0f), XYZ(0f, -1f, 0f), XYZ(0f, 1f, 0f), XYZ(0f, 0f, -1f), XYZ(0f, 0f, 1f)
        ).map { it
            .`🔄Z`(xyRadians)
            .`🔄X`(yzRadians)
            .`🔄Y`(zxRadians)
            .scaled(XYZ(size.width, size.height))
            .scaled(XYZ(10F, 10F))
            .offset(offset)
            .toOffset()
        }

        drawLine(Color.Red, points[0], points[1])
        drawLine(Color.Blue, points[2], points[3])
        drawLine(Color.Green, points[4], points[5])
    }
}

class WorldAssignees(private val world: ComposableWorld) {
    private val offsetAssignee = Assignee.forXYZ(
        "World Offset (three numbers separated by space)",
        { world.offset = it },
        world.offset.toSpaceDelimitedString()
    )
    private val scaleAssignee = Assignee.forXYZ(
        "World Scale (three numbers separated by space)",
        { world.scale = it },
        world.scale.toSpaceDelimitedString()
    )
    private val xyRadiansAssignee = Assignee.forFloat(
        "World Rotation in XY (in degrees)",
        { world.xyRadians = it },
        toDegrees(world.xyRadians.toDouble() % TWO_PI).toString()
    )
    private val yzRadiansAssignee = Assignee.forFloat(
        "World Rotation in YZ (in degrees)",
        { world.yzRadians = it },
        toDegrees(world.yzRadians.toDouble() % TWO_PI).toString()
    )
    private val zxRadiansAssignee = Assignee.forFloat(
        "World Rotation in ZX (in degrees)",
        { world.zxRadians = it },
        toDegrees(world.zxRadians.toDouble() % TWO_PI).toString()
    )

    context(MenuScope)
    @Composable
    fun MenuBarItems() {
        Item(text = "World offset", onClick = offsetAssignee::open)
        Item(text = "World scale", onClick = scaleAssignee::open)
        Item(text = "World XY rotation", onClick = xyRadiansAssignee::open)
        Item(text = "World YZ rotation", onClick = yzRadiansAssignee::open)
        Item(text = "World ZX rotation", onClick = zxRadiansAssignee::open)
    }

    @Composable
    fun Dialogs() {
        offsetAssignee.dialog()
        scaleAssignee.dialog()
        xyRadiansAssignee.dialog()
        yzRadiansAssignee.dialog()
        zxRadiansAssignee.dialog()
    }
}

@Composable
fun rememberWorldAssignees(world: ComposableWorld) = remember(world.key) { WorldAssignees(world) }

private val World.key get() = listOf(offset, scale, xyRadians, yzRadians, zxRadians).hashCode()

fun Offset.toWorldXYZ(world: World) = XYZ.fromOffset(this)
    .offset(world.offset * XYZ(-1f, -1f, 1f))
    .unscaled(world.scale)
    .`🔄Y`(-world.zxRadians)
    .`🔄X`(-world.yzRadians)
    .`🔄Z`(-world.xyRadians)

fun XYZ.toCanvasXYZ(world: World) = this
    .scaled(world.scale)
    .`🔄Z`(world.xyRadians)
    .`🔄X`(world.yzRadians)
    .`🔄Y`(world.zxRadians)
    .offset(world.offset)

fun XYZ.toCanvasOffset(world: World) = toCanvasXYZ(world).toOffset()
