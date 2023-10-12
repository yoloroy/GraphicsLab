package util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import components.TransformationResult
import kotlin.math.max
import kotlin.math.min

fun Rect.Companion.areaOf(a: Offset, b: Offset) = Rect(
    topLeft = Offset(min(a.x, b.x), min(a.y, b.y)),
    bottomRight = Offset(max(a.x, b.x), max(a.y, b.y))
)

fun transformSpaceDelimitedStringToIntSize(string: String): TransformationResult<IntSize> = try {
    TransformationResult.Success(string.split(" ").map { it.toInt() }.let { IntSize(it[0], it[1]) })
} catch (e: NumberFormatException) {
    TransformationResult.FailureMessage("Bad number format")
} catch (e: IndexOutOfBoundsException) {
    TransformationResult.FailureMessage("Not enough numbers are provided")
}
