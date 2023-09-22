package util

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed

val KeyEvent.isWinCtrlPressed get() = when (currentOs) {
    OS.MAC -> isMetaPressed
    else -> isCtrlPressed
}

val KeyEvent.isWinAltPressed get() = when (currentOs) {
    OS.MAC -> isCtrlPressed
    else -> isAltPressed
}

@Suppress("unused")
val KeyEvent.isMacMetaPressed get() = isWinCtrlPressed

@Suppress("unused")
val KeyEvent.isMacCtrlPressed get() = isWinAltPressed
