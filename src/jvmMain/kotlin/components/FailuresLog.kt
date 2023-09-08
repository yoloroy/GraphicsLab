package components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimedValue

val SLOPE_START = 5.seconds
val HIDDEN_START = 10.seconds
val HIDING_DURATION = HIDDEN_START - SLOPE_START

sealed class Failure(val message: String, val color: Color) {
    class UncaughtException(message: String) : Failure(message, Color.Red)
    class Mistake(message: String) : Failure(message, Color.Black)
}

@Composable
fun FailuresLog(failures: List<Failure>, modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()

    var timedFailures by remember { mutableStateOf(listOf<TimedValue<Failure>>()) }
    timedFailures += failures
        .filter { timedFailures.none { (failure, _) -> failure === it } }
        .map { TimedValue(it, System.currentTimeMillis().milliseconds) }

    var hiddenFailures by remember { mutableStateOf(setOf<TimedValue<Failure>>()) }
    val shownFailures = timedFailures
        .minus(hiddenFailures)
        .filter { (System.currentTimeMillis().milliseconds - it.duration) < HIDDEN_START }
        .distinctBy { it.value.message }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.Bottom
    ) {
        items(shownFailures) { timedFailure ->
            val failure = timedFailure.value

            var visible by remember { mutableStateOf(true) }

            AnimatedVisibility(
                visible = visible,
                exit = fadeOut(
                    tween(
                        durationMillis = HIDING_DURATION.inWholeMilliseconds.toInt()
                    )
                )
            ) {
                Text(
                    text = failure.message,
                    color = failure.color,
                    modifier = Modifier.fillParentMaxWidth().padding(8.dp)
                )
            }

            LaunchedEffect(timedFailure) {
                coroutineScope.launch {
                    delay(SLOPE_START)
                    visible = false
                    delay(HIDDEN_START)
                    hiddenFailures += timedFailure
                }
            }
        }
    }
}
