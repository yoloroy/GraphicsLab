package components

import points.ComposableWorld
import points.World
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.window.MenuScope
import common.forFloat
import common.forXYZ
import util.TWO_PI

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
        Math.toDegrees(world.xyRadians.toDouble() % TWO_PI).toString()
    )
    private val yzRadiansAssignee = Assignee.forFloat(
        "World Rotation in YZ (in degrees)",
        { world.yzRadians = it },
        Math.toDegrees(world.yzRadians.toDouble() % TWO_PI).toString()
    )
    private val zxRadiansAssignee = Assignee.forFloat(
        "World Rotation in ZX (in degrees)",
        { world.zxRadians = it },
        Math.toDegrees(world.zxRadians.toDouble() % TWO_PI).toString()
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
