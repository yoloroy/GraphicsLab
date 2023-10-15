package points

class TriangleIndices(
    val ai: Int,
    val bi: Int,
    val ci: Int
) {
    fun isFacingCamera(points: List<XYZ>): Boolean {
        val a = points[ai]
        val b = points[bi]
        val c = points[ci]

        val dx1 = b.x - a.x
        val dx2 = c.x - b.x
        val dy1 = b.y - a.y
        val dy2 = c.y - b.y

        return dx1 * dy2 - dx2 * dy1 >= 0
    }

    fun anyEquals(index: Int) = index == ai || index == bi || index == ci
}

infix fun Iterable<Pair<Int, Int>>.withoutPairsOf(triangles: List<TriangleIndices>): List<Pair<Int, Int>> {
    val pairsFromTriangles = triangles.flatMapTo(mutableSetOf()) {
        sequenceOf(
            setOf(it.ai, it.bi),
            setOf(it.bi, it.ci),
            setOf(it.ci, it.ai)
        )
    }

    return this
        .mapTo(mutableSetOf()) { (ai, bi) -> setOf(ai, bi) }
        .minus(pairsFromTriangles)
        .mapTo(mutableListOf()) { it.toList().let { it[0] to it[1] } }
}