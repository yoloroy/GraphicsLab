package util

fun <T, R> List<Pair<T, T>>.mapBoth(transform: (T) -> R): List<Pair<R, R>> = map { (a, b) -> transform(a) to transform(b) }
