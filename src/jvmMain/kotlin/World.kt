import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
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
    fun menuBarItems() {
        Item(text = "World offset", onClick = offsetAssignee::open)
        Item(text = "World scale", onClick = scaleAssignee::open)
        Item(text = "World XY rotation", onClick = xyRadiansAssignee::open)
        Item(text = "World YZ rotation", onClick = yzRadiansAssignee::open)
        Item(text = "World ZX rotation", onClick = zxRadiansAssignee::open)
    }

    @Composable
    fun dialogs() {
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

fun XYZ.toCanvas(world: World) = this
    .scaled(world.scale)
    .`🔄Z`(world.xyRadians)
    .`🔄X`(world.yzRadians)
    .`🔄Y`(world.zxRadians)
    .offset(world.offset)
    .toOffset()
