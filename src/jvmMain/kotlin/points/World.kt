package points

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import components.Failures

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
                failures.logMistake("points.World scale should be > 0")
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
