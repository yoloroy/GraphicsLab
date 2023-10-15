package canvas

import points.World
import points.XYZ
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import util.*
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

class RayTracingComponent(
    private val canvasPoints: CanvasPoints,
    private val renderScreenSize: IntSize,
    private val world: World,
    private val uiCoroutineScope: CoroutineScope
): PointsCanvas {

    var isPlaying by atomicReference(false)
    private var bitmap by mutableStateOf(null as ImageBitmap?, neverEqualPolicy())

    private val triangles by derivedStateOf { canvasPoints.triangles }
    private var canvasSize by atomicReference(renderScreenSize)

    companion object {
        private var renderThread: Thread? = null
        private const val MAX_FPS = 1
        private const val MAX_MS_PER_FRAME = 1000 / MAX_FPS
        private val BACKGROUND_COLOR = Color.Black
    }

    init {
        thread(false, name = "canvas.RayTracingComponent.Companion.renderThread") {
            val result = ImageBitmap(renderScreenSize.width, renderScreenSize.height)
            val canvas = Canvas(result)

            uiCoroutineScope.launch { bitmap = result }

            while (renderThread == Thread.currentThread()) {
                if (!isPlaying) {
                    Thread.sleep(MAX_MS_PER_FRAME.toLong())
                    continue
                }
                val scale = renderScreenSize.run {
                    Offset(canvasSize.width.toFloat() / width, canvasSize.height.toFloat() / height)
                }
                val dt = measureTimeMillis dt@{
                    val triangles = triangles.takeIfNotEmpty() ?: return@dt
                    canvas.renderTriangles(triangles, scale)
                    uiCoroutineScope.launch { bitmap = result }
                }
                println("frame is rendered in $dt ms")
                if (dt < MAX_MS_PER_FRAME) {
                    Thread.sleep(MAX_MS_PER_FRAME - dt)
                }
            }
        }.apply {
            renderThread = this
            start()
        }
    }

    private fun Canvas.renderTriangles(triangles: List<TriangleForRender>, scale: Offset) {
        forEachBlock(
            renderScreenSize,
            scale,
            triangles,
            rows = 20, // TODO Assignee for rows and columns
            columns = 20
        ) { blockTriangles, block ->

            if (!isPlaying) return
            if (blockTriangles.isEmpty()) {
                drawRect(block.toRect(), paint { color = BACKGROUND_COLOR })
                return@forEachBlock
            }

            block.forEachPixel { x, y ->
                val paint = paint {
                    color = castRay(
                        triangles = blockTriangles,
                        start = XYZ(x * scale.x, y * scale.y, 0f),
                        direction = XYZ(0f, 0f, 1f)
                    ) ?: BACKGROUND_COLOR
                }
                drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
            }
        }

        save()
    }

    @Composable
    override fun View(modifier: Modifier) {
        bitmap?.let {
            Image(
                it,
                "rendered mesh",
                contentScale = ContentScale.FillBounds,
                modifier = modifier
                    .background(BACKGROUND_COLOR)
                    .onSizeChanged { canvasSize = it }
            )
        }
    }

    fun copyWithOtherScreenSize(screenSize: IntSize) = RayTracingComponent(canvasPoints, screenSize, world, uiCoroutineScope)
}

private inline fun forEachBlock(
    renderScreenSize: IntSize,
    scale: Offset,
    triangles: List<TriangleForRender>,
    rows: Int,
    columns: Int,
    onEachBlock: (List<TriangleForRender>, bounds: IntRect) -> Unit = { _, _ -> }
) {
    val blockSize = IntSize(renderScreenSize.width / columns, renderScreenSize.height / rows)
    val screenBlockSize = Size(renderScreenSize.width * scale.x / columns, renderScreenSize.height * scale.y / rows)

    for (by in 0 until rows) {
        for (bx in 0 until columns) {
            val blockTopLeft = IntOffset(bx * blockSize.width, by * blockSize.height)
            val blockBounds = IntRect(blockTopLeft, blockSize)
            val screenTopLeft = Offset(bx * screenBlockSize.width, by * screenBlockSize.height)
            val screenBlockBounds = Rect(screenTopLeft, screenBlockSize)
            val blockTriangles = triangles.filter { it.overlappedBy(screenBlockBounds) }

            onEachBlock(blockTriangles, blockBounds)
        }
    }
}

private fun castRay(triangles: List<TriangleForRender>, start: XYZ, direction: XYZ): Color? {
    var closestTriangle = null as TriangleForRender?
    var minT = Float.POSITIVE_INFINITY

    for (triangle in triangles) {
        val t = triangle.tForRayOrNull(start, direction) ?: continue
        if (-t < minT) {
            minT = -t
            closestTriangle = triangle
        }
    }

    if (closestTriangle == null) return null

    val shade = closestTriangle.facingValue
    return Color(shade, shade, shade, 1f)
}
