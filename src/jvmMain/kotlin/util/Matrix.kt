package util

import kotlin.math.*

// this is bad file that should be moved to separate class

typealias Matrix = List<List<Float>>

fun xyRotationMatrix(radians: Float): Matrix = listOf(
    listOf(cos(radians) , sin(radians), 0F, 0F),
    listOf(-sin(radians), cos(radians), 0F, 0F),
    listOf(      0F     ,      0F     , 1F, 0F),
    listOf(      0F     ,      0F     , 0F, 1F)
)

fun yzRotationMatrix(radians: Float): Matrix = listOf(
    listOf(1F, 0F, 0F, 0F),
    listOf(0F, cos(radians), sin(radians), 0F),
    listOf(      0F     ,      sin(radians), cos(radians), 0F),
    listOf(      0F     ,      0F     , 0F, 1F)
)

fun zxRotationMatrix(radians: Float): Matrix = listOf(
    listOf(cos(radians), -sin(radians), 0F, 0F),
    listOf(0F, 1F, 0F, 0F),
    listOf(sin(radians), cos(radians), 1F, 0F),
    listOf(0F, 0F, 0F, 1F)
)

fun combinedRotationMatrix(xyRadians: Float, yzRadians: Float, zxRadians: Float) =
    xyRotationMatrix(xyRadians) * yzRotationMatrix(yzRadians) * zxRotationMatrix(zxRadians)

operator fun Matrix.times(other: Matrix): Matrix = map { aRow ->
    other.transposed().map { bCol ->
        (aRow zip bCol)
            .map { (a, b) -> a * b }
            .sum()
    }
}

fun Matrix.transposed(): Matrix = List(firstOrEmpty().size) { x -> List(size) { y -> this[y][x] } }

/**
 * According to http://eecs.qmul.ac.uk/~gslabaugh/publications/euler.pdf
 * rotation matrix can be seen like this:
 * ``` kotlin
 * listOf(
 *  listOf(cos(🔄zx) * cos(🔄xy), ..., ..., ...),
 *  listOf(..., ..., ..., ...),
 *  listOf(-sin(🔄zx), sin(🔄yz) * cos(🔄zx), ..., ...),
 *  listOf(..., ..., ..., ..., ...)
 * )
 * ```
 * And from the above matrix angles in radians will be retrieved
 *
 * @return list which represents listOf(🔄xy, 🔄yz, 🔄zx) in radians
 */
fun retrieveNiceXYZAnglesFromRotationMatrix(rotationMatrix: Matrix): List<Float> {
    var y = asin(-rotationMatrix[2][0])
    if (y < 0f) y += PI.toFloat() * 2
    var z = acos(rotationMatrix[0][0] / cos(y))
    if (z < 0f) z += PI.toFloat() * 2
    var x = asin(rotationMatrix[2][1] / cos(y))
    if (x < 0f) x += PI.toFloat() * 2
    return listOf(x, y, z)
}
