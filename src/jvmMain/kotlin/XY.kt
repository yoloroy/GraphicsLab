import androidx.compose.ui.geometry.Offset
import kotlinx.serialization.Serializable
import util.times
import util.xyRotationMatrix

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

    fun toMatrix() = listOf(list.map { it.toFloat() })

    fun toOtherMatrix() = list.map { listOf(it.toFloat()) }

    operator fun unaryMinus() = XY(-x, -y)

    operator fun plus(other: XY) = XY(x + other.x, y + other.y)

    operator fun minus(other: XY) = this + (-other)

    operator fun times(other: XY) = toMatrix() * other.toOtherMatrix()

    fun rotatedXY(radians: Float) = XY((toMatrix() * xyRotationMatrix(radians))[0].map { it.toInt() })

    fun distanceSquaredTo(other: XY) = (this - other).run { x * x + y * y }
}
