package input

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

class ComposableCursor: Cursor {
    override var position by mutableStateOf(Offset.Zero)
}
