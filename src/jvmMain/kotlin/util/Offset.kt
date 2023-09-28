package util

import androidx.compose.ui.geometry.Offset

infix fun Offset.distanceTo(other: Offset) = (this - other).getDistance()
