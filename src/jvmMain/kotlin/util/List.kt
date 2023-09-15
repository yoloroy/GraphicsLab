package util

fun <T> List<List<T>>.firstOrEmpty() = firstOrNull().orEmpty()
