package util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

fun <T> MutableSharedFlow<T>.emitter(coroutineScope: CoroutineScope): (T) -> Unit = { value: T ->
    coroutineScope.launch {
        emit(value)
    }
}
