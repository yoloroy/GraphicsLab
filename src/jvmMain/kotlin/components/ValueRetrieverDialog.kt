package components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog

class Assignee<T>(
    private val name: String,
    val transform: (String) -> TransformationResult<T>,
    val assign: (T) -> Unit,
    startValue: String
) {
    private var input by mutableStateOf(startValue)
    private var dialogOpen by mutableStateOf(false)
    private var failMessage by mutableStateOf(null as String?)

    companion object

    fun open() {
        dialogOpen = true
    }

    @Composable
    fun dialog() {
        if (!dialogOpen) return

        Dialog(
            visible = true,
            title = name,
            onCloseRequest = ::closeWithoutInput
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    isError = failMessage != null,
                    label = label@ {
                        val label = failMessage ?: return@label
                        Text(label)
                    },
                    maxLines = 1
                )

                TextButton(
                    onClick = { tryCloseWithInput() }
                ) {
                    Text("OK")
                }
            }
        }
    }

    private fun tryCloseWithInput() {
        when (val result = transform(input)) {
            is TransformationResult.Success -> {
                assign(result.value)
                dialogOpen = false
            }
            is TransformationResult.FailureMessage -> {
                failMessage = result.message
            }
        }
    }

    private fun closeWithoutInput() {
        dialogOpen = false
    }
}

sealed class TransformationResult<T> {
    class Success<T>(val value: T): TransformationResult<T>()
    class FailureMessage<T>(val message: String): TransformationResult<T>()
}
