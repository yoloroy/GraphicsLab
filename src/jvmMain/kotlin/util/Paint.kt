package util

import androidx.compose.ui.graphics.Paint

inline fun paint(block: Paint.() -> Unit) = Paint().apply(block)