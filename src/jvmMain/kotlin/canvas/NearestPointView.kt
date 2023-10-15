package canvas

import androidx.compose.ui.graphics.drawscope.DrawScope

interface NearestPointView {
    context(DrawScope)
    fun drawPathToCursor()
}
