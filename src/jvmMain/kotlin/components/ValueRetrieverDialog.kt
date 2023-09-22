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

@Composable
fun <T> ValueRetrieverDialog(
    visible: Boolean,
    transform: (String) -> T?,
    title: String = "Put in me",
    setValueAndCloseDialog: (T) -> Unit,
    close: () -> Unit,
    startValue: String
) {
    var input by remember(startValue) { mutableStateOf(startValue) }
    var fail by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(input) {
        fail = ""
    }

    if (!visible) return

    Dialog(
        visible = true,
        title = title,
        onCloseRequest = close
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            TextField(
                value = input,
                onValueChange = { input = it },
                isError = fail != null,
                label = label@ {
                    val label = fail ?: return@label
                    Text(label)
                },
                maxLines = 1
            )

            TextButton(
                onClick = {
                    transform(input)?.let {
                        setValueAndCloseDialog(it)
                    } ?: run {
                        fail = "Bad input"
                    }
                }
            ) {
                Text("OK")
            }
        }
    }
}
