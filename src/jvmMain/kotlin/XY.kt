import androidx.compose.ui.geometry.Offset
import kotlinx.serialization.Serializable
import kotlin.math.cos
import kotlin.math.sin

@Serializable
@JvmInline
value class XY(val list: List<Int>) {

    init {
        require(list.size == 3)
    }

    val x: Int get() = list[0]
    val y: Int get() = list[1]

    constructor(x: Int, y: Int) : this(listOf(x, y, 1))

    companion object {
        fun fromOffset(offset: Offset) = XY(offset.x.toInt(), offset.y.toInt())
    }

    fun toOffset() = Offset(x.toFloat(), y.toFloat())

    operator fun unaryMinus() = XY(-x, -y)

    operator fun plus(other: XY) = XY(x + other.x, y + other.y)

    operator fun minus(other: XY) = this + (-other)

    operator fun times(other: XY) = XY(x * other.x, y * other.y)

    operator fun div(other: XY) = XY(x / other.x, y / other.y)

    infix fun `🔄`(xyRotation: Float): XY {
        val c = sin(xyRotation)
        val s = cos(xyRotation)
        val xNew = x * c - y * s
        val yNew = x * s + y * c
        return XY(xNew.toInt(), yNew.toInt())
    }

    fun distanceSquaredTo(other: XY) = (this - other).run { x * x + y * y }
}
