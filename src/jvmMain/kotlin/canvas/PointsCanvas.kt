package canvas

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface PointsCanvas {
    @Composable
    fun View(modifier: Modifier)
}
