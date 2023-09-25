package util

import kotlin.reflect.KProperty

fun <T, R> ((T) -> Unit).returning(constant: R) = lambda@ { value: T ->
    invoke(value)
    return@lambda constant
}

operator fun <T> (() -> T).getValue(thisObj: Any?, property: KProperty<*>) = invoke()
