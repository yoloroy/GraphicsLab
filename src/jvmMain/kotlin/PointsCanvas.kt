import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import components.CursorInput
import components.diff
import components.handleCursorInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import util.filterAny
import util.map
import util.takeIfNotEmpty
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.system.measureTimeMillis

interface PointsCanvas {
    @Composable
    fun View(modifier: Modifier)
}

class ComposablePointsCanvas(
    private val isTransparentBuild: Boolean,
    private val cursorInput: CursorInput,
    private val points: CanvasPoints,
    private val selection: SelectedPoints,
    private val nearestPoint: NearestPoint,
    trianglesComponent: PointsCanvas
): PointsCanvas {

    var trianglesComponent by mutableStateOf(trianglesComponent)

    @Composable
    override fun View(modifier: Modifier) {
        Box(
            modifier
                .background(if (isTransparentBuild) Color(0x44ffffff) else Color.White)
                .handleCursorInput(cursorInput)
        ) {
            trianglesComponent.View(Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize()) {
                // TODO move
                for (index in selection.selected) {
                    val isSelectionPosOffset = cursorInput.dragging && cursorInput.mode is CursorInput.Drag
                    val point = when (true) {
                        isSelectionPosOffset -> points.offsets[index] + cursorInput.dragState.diff
                        else -> points.offsets[index]
                    }
                    drawCircle(Color.Black, 4F, point, style = Stroke(1f))
                }

                if (!cursorInput.dragging) {
                    nearestPoint.drawPathToCursor()
                }
                cursorInput.draw()
            }
        }
    }
}

class WireframeComponent(
    private val points: CanvasPoints,
    private val world: World
): PointsCanvas {

    @Composable
    override fun View(modifier: Modifier) {
        Canvas(modifier) {
            world.drawCoordinateAxes()

            for ((ai, bi, ci) in points.triangles) {
                val a = points.offsets[ai]
                val b = points.offsets[bi]
                val c = points.offsets[ci]
                drawLine(Color.Black, a, b)
                drawLine(Color.Black, b, c)
                drawLine(Color.Black, c, a)
            }

            for ((ai, bi) in points.connections.filterAny { it in points.nonTriangles }) {
                drawLine(Color.Red, points.offsets[ai], points.offsets[bi])
            }

            for (point in points.offsets) {
                drawCircle(Color.Black, 2F, point)
            }
        }
    }
}

class RayTracingComponent(
    private val canvasPoints: CanvasPoints,
    private val renderScreenSize: IntSize,
    private val world: World,
    private val uiCoroutineScope: CoroutineScope
): PointsCanvas {

    var isPlaying = false
    private var bitmap by mutableStateOf(null as ImageBitmap?, neverEqualPolicy())

    private var savedPoints = canvasPoints.points
    private var savedTriangles = canvasPoints.triangles

    private var canvasSize = renderScreenSize

    companion object {
        private var renderThread: Thread? = null
        private const val MAX_FPS = 1
        private const val MAX_MS_PER_FRAME = 1000 / MAX_FPS
        private val BACKGROUND_COLOR = Color.Black
    }

    init {
        thread(false, name = "RayTracingComponent.Companion.renderThread") {
            val result = ImageBitmap(renderScreenSize.width, renderScreenSize.height)
            val canvas = Canvas(result)

            uiCoroutineScope.launch { bitmap = result }

            while (renderThread == Thread.currentThread()) {
                if (!isPlaying) {
                    Thread.sleep(MAX_MS_PER_FRAME.toLong())
                    continue
                }
                val dt = measureTimeMillis dt@ {
                    val points = savedPoints.takeIfNotEmpty() ?: return@dt
                    val triangles = savedTriangles.takeIfNotEmpty() ?: return@dt
                    render(canvas, points, triangles)
                    uiCoroutineScope.launch { bitmap = result }
                }
                if (dt < MAX_MS_PER_FRAME) {
                    Thread.sleep(MAX_MS_PER_FRAME - dt)
                }
            }
        }.apply {
            renderThread = this
            start()
        }
    }

    private fun render(
        onto: Canvas,
        points: List<XYZ>,
        trianglesIndices: List<Triple<Int, Int, Int>>
    ) {
        val triangles = trianglesIndices.map { it.map { i -> points[i] } }
        val normals = calculateNormals(triangles)
        val normalsDotAs = calculateNormalDotAs(triangles, normals)
        val bsMinusAs = triangles.map { (a, b, _) -> b - a }
        val csMinusBs = triangles.map { (_, b, c) -> c - b }
        val asMinusCs = triangles.map { (a, _, c) -> a - c }

        onto.apply {
            val scale = renderScreenSize.run {
                Size(canvasSize.width.toFloat() / width, canvasSize.height.toFloat() / height)
            }

            for (y in 0 until renderScreenSize.height) {
                if (!isPlaying) return
                for (x in 0 until renderScreenSize.width) {
                    val start = XYZ(x * scale.width, y * scale.height, 0f)
                    measureTimeMillis {
                        drawRect(
                            x.toFloat(),
                            y.toFloat(),
                            x.toFloat() + 1,
                            y.toFloat() + 1,
                            paint = Paint().apply {
                                color = castRay(
                                    triangles,
                                    start, XYZ(0f, 0f, 1f),
                                    normals, normalsDotAs,
                                    bsMinusAs, csMinusBs, asMinusCs
                                )
                            }
                        )
                    }.let { println("ray[$y][$x] was checked in $it ms") }
                }
            }

            save()
        }
    }

    private fun castRay(
        triangles: List<Triple<XYZ, XYZ, XYZ>>,
        start: XYZ,
        direction: XYZ,
        normals: List<XYZ>,
        normalsDotAs: List<Float>,
        bsMinusAs: List<XYZ>,
        csMinusBs: List<XYZ>,
        asMinusCs: List<XYZ>
    ): Color {
        var closestTriangle = null as Triple<XYZ, XYZ, XYZ>?
        var minT = Float.POSITIVE_INFINITY

        for ((i, triangle) in triangles.withIndex()) {
            val t = triangle.timeForRayOrNull(
                start, direction,
                normals[i], normalsDotAs[i],
                bsMinusAs[i], csMinusBs[i], asMinusCs[i]
            ) ?: continue
            if (-t < minT) {
                minT = -t
                closestTriangle = triangle
            }
        }

        if (closestTriangle == null) return BACKGROUND_COLOR

        val shade = closestTriangle.normal().normalized().z * 0.5f + 0.5f
        return Color(shade, shade, shade, 1f)
    }

    private fun calculateNormals(triangles: List<Triple<XYZ, XYZ, XYZ>>) = triangles.map { it.normal() }

    private fun calculateNormalDotAs(
        triangles: List<Triple<XYZ, XYZ, XYZ>>,
        normals: List<XYZ>
    ) = triangles.mapIndexed { i, (a, _, _) -> normals[i] `•` a }

    private fun Triple<XYZ, XYZ, XYZ>.normal() = (second - first) `×` (third - first)

    private fun Triple<XYZ, XYZ, XYZ>.timeForRayOrNull(
        start: XYZ, direction: XYZ,
        normal: XYZ, normalDotA: Float,
        bMinusA: XYZ, cMinusB: XYZ, aMinusC: XYZ
    ): Float? {
        val nDotRayDirection = normal `•` direction

        if (abs(nDotRayDirection) < 0.05) return null // TODO refactor magic constant close to zero

        val t = -((normal `•` start) - normalDotA) / nDotRayDirection
        val intersection = start + direction * t

        return t.takeUnless {
            normal `•` (bMinusA `×` (intersection - first)) < 0 ||
            normal `•` (cMinusB `×` (intersection - second)) < 0 ||
            normal `•` (aMinusC `×` (intersection - third)) < 0
        }
    }

    @Composable
    override fun View(modifier: Modifier) {
        LaunchedEffect(canvasPoints.points) { savedPoints = canvasPoints.points }
        LaunchedEffect(canvasPoints.triangles) { savedTriangles = canvasPoints.triangles }

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
