package util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.max
import kotlin.math.min

fun Rect.Companion.areaOf(a: Offset, b: Offset) = Rect(
    topLeft = Offset(min(a.x, b.x), min(a.y, b.y)),
    bottomRight = Offset(max(a.x, b.x), max(a.y, b.y))
)