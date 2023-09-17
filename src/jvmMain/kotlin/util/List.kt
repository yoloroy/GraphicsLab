package util

fun <T1, T2, R> List<T1>.zipAsSequence(other: List<T2>, transform: (T1, T2) -> R) = asSequence().zip(other.asSequence(), transform)
