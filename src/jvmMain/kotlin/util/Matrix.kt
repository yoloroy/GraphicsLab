package util

import kotlin.math.cos
import kotlin.math.sin

fun xyRotationMatrix(radians: Float) = listOf(
    listOf(cos(radians) , sin(radians), 0F),
    listOf(-sin(radians), cos(radians), 0F),
    listOf(      0F     ,      0F     , 1F)
)

operator fun List<List<Float>>.times(other: List<List<Number>>) = indices.map { y ->
    other.first().indices.map { x ->
        this[y].zip(other.map { it[x] }) { a, b -> a * b.toFloat() }.sum()
    }
}
