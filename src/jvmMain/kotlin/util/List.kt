package util

fun <T1, T2, R> Iterable<T1>.zipAsSequence(other: Iterable<T2>, transform: (T1, T2) -> R) = asSequence().zip(other.asSequence(), transform)

fun <T, R: Comparable<R>> List<T>.indexOfMinBy(selector: (T) -> R) = indexOf(minBy(selector))

fun <T> List<T>.dropAt(index: Int): List<T> = when {
    isEmpty() -> throw IndexOutOfBoundsException()
    index == 0 && size == 1 -> emptyList()
    index == 0 -> drop(1)
    index == lastIndex -> dropLast(1)
    else -> slice(0 until index) + slice(index + 1..lastIndex)
}

fun <T> List<T>.takeIfNotEmpty() = takeIf { isNotEmpty() }

fun <T> List<T>.combinationsOfPairs() = flatMapIndexed { ai, a -> List(lastIndex - ai) { i -> a to this[i + ai + 1] } }

fun <T> List<IndexedValue<T>>.retrieveIndices() = map { it.index }

inline fun <T> List<Pair<T, T>>.filterBoth(crossinline predicate: (T) -> Boolean) = filter { predicate(it.first) && predicate(it.second) }

inline fun <T> Sequence<Pair<T, T>>.filterBoth(crossinline predicate: (T) -> Boolean) = filter { predicate(it.first) && predicate(it.second) } // TODO move
