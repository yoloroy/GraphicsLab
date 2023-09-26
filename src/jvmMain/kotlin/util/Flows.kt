package util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.launch

fun <T> MutableSharedFlow<T>.createEmmitterIn(coroutineScope: CoroutineScope): (T) -> Unit = { value: T ->
    coroutineScope.launch {
        emit(value)
    }
}

fun <T> Flow<T>.partition(predicate: (T) -> Boolean) = filter(predicate) to filterNot(predicate)
