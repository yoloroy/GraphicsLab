package components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.*
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

    override fun hashCode() = message.hashCode() + color.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other as? Failure == null) return false
        if (message != other.message) return false
        return color == other.color
    }
}

@Composable
fun FailuresLog(failures: List<Failure>, modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()

    var timedFailures by remember { mutableStateOf(listOf<TimedValue<Failure>>()) }
    timedFailures += failures
        .filter { timedFailures.none { (failure, _) -> failure === it } }
        .map { TimedValue(it, System.currentTimeMillis().milliseconds) }
        .distinctBy { it.value.message.hashCode() }

    var hiddenFailures by remember { mutableStateOf(setOf<TimedValue<Failure>>()) }
    val shownFailures = (timedFailures - hiddenFailures)
        .distinctBy { it.duration.inWholeSeconds / SLOPE_START.inWholeSeconds }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Bottom
    ) {
        for (timedFailure in shownFailures) {
            val failure = timedFailure.value
            var visible by remember { mutableStateOf(true) }

            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(),
                exit = fadeOut(tween(durationMillis = HIDING_DURATION.inWholeMilliseconds.toInt()))
            ) {
                Text(
                    text = failure.message,
                    color = failure.color,
                    modifier = Modifier.width(IntrinsicSize.Max).padding(8.dp)
                )
            }

            LaunchedEffect(timedFailure) {
                coroutineScope.launch {
                    delay(SLOPE_START)
                    visible = false
                    delay(HIDING_DURATION)
                    hiddenFailures += timedFailure
                }
            }
        }
    }
}
