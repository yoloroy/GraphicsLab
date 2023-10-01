package components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
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

private val SLOPE_START = 5.seconds
private val HIDDEN_START = 10.seconds
private val HIDING_DURATION = HIDDEN_START - SLOPE_START

interface Failures {

    fun logMistake(message: String)

    fun logException(message: String)

    @Composable
    fun Console(modifier: Modifier)
}

class ComposableFailures: Failures {

    private var failures by mutableStateOf(emptyList<Type>())

    override fun logMistake(message: String) {
        failures += Type.Mistake(message)
    }

    override fun logException(message: String) {
        failures += Type.Exception(message)
    }

    @Composable
    override fun Console(modifier: Modifier) {
        val coroutineScope = rememberCoroutineScope()

        var timedFailures by remember { mutableStateOf(listOf<TimedValue<Type>>()) }
        timedFailures += failures
            .takeLastWhile { timedFailures.none { (failure, _) -> failure === it } }
            .distinctBy { it.message }
            .map { TimedValue(it, System.currentTimeMillis().milliseconds) }

        var hiddenFailures by remember { mutableStateOf(setOf<TimedValue<Type>>()) }
        val shownFailures = (timedFailures - hiddenFailures)
            .distinctBy { it.value.message.hashCode() + it.duration.inWholeSeconds / SLOPE_START.inWholeSeconds }

        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Bottom
        ) {
            for (timedFailure in shownFailures) {
                val failure = timedFailure.value
                var visible by remember { mutableStateOf(true) }

                AnimatedVisibility(
                    visible = visible,
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

    private sealed class Type(val message: String, val color: Color) {
        class Exception(message: String) : Type(message, Color.Red)
        class Mistake(message: String) : Type(message, Color.Black)

        override fun hashCode() = message.hashCode() + color.hashCode()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other as? Type == null) return false
            if (message != other.message) return false
            return color == other.color
        }
    }
}
