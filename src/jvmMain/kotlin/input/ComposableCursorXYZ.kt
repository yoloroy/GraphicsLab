package input

import points.World
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import points.toWorldXYZ

class ComposableCursorXYZ(private val cursor: Cursor, private val world: World): CursorXYZ {
    override val position by derivedStateOf { cursor.position.toWorldXYZ(world) }
}
