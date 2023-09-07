import androidx.compose.ui.geometry.Offset
import kotlinx.serialization.Serializable

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
}
