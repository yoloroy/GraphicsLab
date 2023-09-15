import androidx.compose.ui.geometry.Offset
import kotlinx.serialization.Serializable
import util.times
import util.xyRotationMatrix
import util.yzRotationMatrix
import util.zxRotationMatrix

@Serializable
@JvmInline
value class XYZ(val list: List<Float>) {

    init {
        require(list.size == 4)
    }

    val x: Float get() = list[0]
    val y: Float get() = list[1]
    val z: Float get() = list[2]

    constructor(x: Float, y: Float) : this(listOf(x, y, 1F, 1F))

    constructor(x: Float, y: Float, z: Float = 1F) : this(listOf(x, y, z, 1F))

    companion object {
        fun fromOffset(offset: Offset) = XYZ(offset.x, offset.y)
    }

    fun toOffset() = Offset(x, y)

    fun toMatrix() = list.map { listOf(it) } // matrix with one row of size 4

    fun toOtherMatrix() = listOf(list) // matrix of size 4 with one column

    operator fun unaryMinus() = XYZ(-x, -y)

    operator fun plus(other: XYZ) = XYZ(x + other.x, y + other.y)

    operator fun minus(other: XYZ) = this + (-other)

    operator fun times(other: XYZ) = XYZ((toMatrix() * other.toOtherMatrix()).map { it[0] })

    infix fun `🔄Z`(radians: Float) = XYZ((toOtherMatrix() * xyRotationMatrix(radians))[0])

    infix fun `🔄Y`(radians: Float) = XYZ((toOtherMatrix() * zxRotationMatrix(radians))[0])

    infix fun `🔄X`(radians: Float) = XYZ((toOtherMatrix() * yzRotationMatrix(radians))[0])

    fun distanceSquaredTo(other: XYZ) = (this - other).run { x * x + y * y + z * z }
}
