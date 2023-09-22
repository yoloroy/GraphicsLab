package util

import androidx.compose.ui.geometry.Offset

fun Offset.distanceTo(other: Offset) = (this - other).getDistance()
