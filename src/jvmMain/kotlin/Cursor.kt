import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

interface Cursor {
    val position: Offset
}

class ComposableCursor: Cursor {
    override var position by mutableStateOf(Offset.Zero)
}
