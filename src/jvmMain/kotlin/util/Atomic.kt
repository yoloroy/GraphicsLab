package util

import java.util.concurrent.atomic.AtomicReference
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Delegates a field to an AtomicReference.
 *
 * Taken from https://gist.github.com/alexfacciorusso/f9f7ab1c9aec0815ee161443bfb5f178
 * @author https://github.com/alexfacciorusso
 */
fun <T> atomicReference(value: T): ReadWriteProperty<Any?, T> = object : ReadWriteProperty<Any?, T> {
    private val atomic = AtomicReference(value)

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T = atomic.get()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = atomic.set(value)
}
