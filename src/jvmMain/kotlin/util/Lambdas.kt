package util

fun <T, R> ((T) -> Unit).returning(constant: R) = lambda@ { value: T ->
    invoke(value)
    return@lambda constant
}
