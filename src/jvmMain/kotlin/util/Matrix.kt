package util

import kotlin.math.*

// this is bad file that should be moved to separate class

typealias Matrix = List<List<Float>>

fun xyRotationMatrix(radians: Float): Matrix = listOf(
    listOf(cos(radians), -sin(radians), 0F, 0F),
    listOf(sin(radians), cos(radians), 0F, 0F),
    listOf(0F, 0F, 1F, 0F),
    listOf(0F, 0F, 0F, 1F)
)

fun yzRotationMatrix(radians: Float): Matrix = listOf(
    listOf(1F, 0F, 0F, 0F),
    listOf(0F, cos(radians), -sin(radians), 0F),
    listOf(0F, sin(radians), cos(radians), 0F),
    listOf(0F, 0F, 0F, 1F)
)

fun zxRotationMatrix(radians: Float): Matrix = listOf(
    listOf(cos(radians), 0F, sin(radians), 0F),
    listOf(0F, 1F, 0F, 0F),
    listOf(-sin(radians), 0F, cos(radians), 0F),
    listOf(0F, 0F, 0F, 1F)
)

operator fun Matrix.times(other: Matrix) = indices.map { y ->
    other.first().indices.map { x ->
        this[y].zipAsSequence(other) { a, bRow -> a * bRow[x] }.sum()
    }
}
