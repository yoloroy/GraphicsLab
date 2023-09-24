import androidx.compose.ui.geometry.Offset
import kotlinx.serialization.Serializable
import util.times
import util.xyRotationMatrix
import util.yzRotationMatrix
import util.zxRotationMatrix

@Serializable
@JvmInline
value class XYZ(private val list: List<Float>) {

    init {
        require(list.size == 4)
    }

    val x: Float get() = list[0]
    val y: Float get() = list[1]
    val z: Float get() = list[2]

    constructor(x: Float, y: Float) : this(listOf(x, y, 1F, 1F))

    constructor(x: Float, y: Float, z: Float = 1F) : this(listOf(x, y, z, 1F))

    companion object {
        fun fromOffset(offset: Offset) = XYZ(offset.x, offset.y, 0f)

        val ZERO = XYZ(0F, 0F, 0F)

        val ONE = XYZ(1F, 1F, 1F)
    }

    fun toOffset() = Offset(x, y)

    private fun toMatrix() = list.map { listOf(it) } // matrix of size 4 with one column

    private fun toOtherMatrix() = listOf(list) // matrix with one row of size 4

    operator fun unaryMinus() = XYZ(list.map { -it })

    operator fun plus(other: XYZ) = XYZ(list.zip(other.list) { a, b -> a + b })

    operator fun minus(other: XYZ) = this + (-other)

    operator fun times(other: XYZ) = XYZ((toMatrix() * other.toOtherMatrix()).map { it[0] })

    operator fun times(factor: Float) = XYZ(x * factor, y * factor, z * factor)

    operator fun div(factor: Float) = times(1 / factor)

    infix fun scaled(other: XYZ) = this * other

    infix fun unscaled(other: XYZ) = this * XYZ(other.list.map { 1 / it })

    infix fun offset(other: XYZ) = this + other

    infix fun `🔄Z`(radians: Float) = XYZ((toOtherMatrix() * xyRotationMatrix(radians))[0])

    infix fun `🔄Y`(radians: Float) = XYZ((toOtherMatrix() * zxRotationMatrix(radians))[0])

    infix fun `🔄X`(radians: Float) = XYZ((toOtherMatrix() * yzRotationMatrix(radians))[0])

    fun copy(x: Float = this.x, y: Float = this.y, z: Float = this.z) = XYZ(x, y, z)
}

fun List<XYZ>.sum() = reduce(XYZ::plus)

fun List<XYZ>.average() = sum() / size.toFloat()

fun Offset.toWorldXYZ(
    worldOffset: XYZ,
    worldScale: XYZ,
    worldXYRotation: Float,
    worldYZRotation: Float,
    worldZXRotation: Float
) = XYZ.fromOffset(this)
    .offset(worldOffset * XYZ(-1f, -1f, 1f))
    .unscaled(worldScale)
    .`🔄Y`(-worldZXRotation)
    .`🔄X`(-worldYZRotation)
    .`🔄Z`(-worldXYRotation)
