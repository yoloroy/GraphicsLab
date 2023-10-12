package util

inline fun <T, R> Triple<T, T, T>.map(transform: (T) -> R) = Triple(transform(first), transform(second), transform(third))
