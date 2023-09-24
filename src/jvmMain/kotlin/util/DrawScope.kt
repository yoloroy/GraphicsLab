package util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.atan2

fun DrawScope.drawArrow(color: Color, start: Offset, end: Offset) {
    drawLine(color, start, end, 1f)
    drawArrowHead(color, end, Size(8f, 16f), atan2(end.y - start.y, end.x - start.x) - PI.toFloat() / 2)
}

private fun DrawScope.drawArrowHead(color: Color, center: Offset, size: Size, radians: Float) {
    val baseTriangle = Path().apply {
        moveTo(0f, 0f)
        lineTo(1f, -1f)
        lineTo(-1f, -1f)
        close()
    }

    translate(center.x, center.y) {
        rotateRad(radians, Offset.Zero) {
            scale(size.width, size.height, Offset.Zero) {
                drawPath(baseTriangle, color)
            }
        }
    }
}
