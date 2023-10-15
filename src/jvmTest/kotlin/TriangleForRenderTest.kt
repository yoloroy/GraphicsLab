import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class TriangleForRenderTest {

    @Test
    fun overlapsBlock() {
        /*
        +------/
        |  +-X--+
        |  X ✅ |
        |/ +----+

                  +----+
                  | ❎ |
                  +----+
        */

        val a = run {
            val points = listOf(XYZ(0f, 0f, 0f), XYZ(0f, 10f, 0f), XYZ(10f, 0f, 0f))
            TriangleForRender(TriangleIndices(0, 1, 2), points)
        }
        val overlapsOnA = Rect(Offset(1f, 1f), Size(10f, 10f))
        val notOverlapsOnA = Rect(Offset(20f, 20f), Size(10f, 10f))

        assertTrue(a.overlappedBy(overlapsOnA))
        assertFalse(a.overlappedBy(notOverlapsOnA))
    }

    @Test
    fun tForRayOrNull() {
        val points = listOf(XYZ(0f, 0f, 0f), XYZ(0f, 10f, 0f), XYZ(10f, 0f, 0f))
        val a = TriangleForRender(TriangleIndices(0, 1, 2), points)

        assertNull(a.tForRayOrNull(-XYZ.ONE, XYZ(0f, 0f, 1f)))
        assertNotNull(a.tForRayOrNull(XYZ.ZERO, XYZ(0f, 0f, 1f)))
        assertEquals(0f, a.tForRayOrNull(XYZ.ZERO, XYZ(0f, 0f, 1f))!!, 0.01f)
    }
}
