@file:Suppress("SameParameterValue")

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import kotlin.math.PI

class WorldInputTarget(
    private val world: ComposableWorld,
    private val movementStep: Float,
    private val scaleStep: Float,
    private val rotationStep: Float
) {
    private val overrides = mapOf(
        Key.One to { setWorldRotation(0, 0, 0) },
        Key.Two to { setWorldRotation(0, 0, PI / 2) },
        Key.Three to { setWorldRotation(0, PI / 2, 0) },
        Key.Four to { setWorldRotation(0, PI / 4, PI / 4) },
        Key.W to { world.offset += XYZ.ZERO.copy(y = -movementStep) },
        Key.A to { world.offset += XYZ.ZERO.copy(x = -movementStep) },
        Key.S to { world.offset += XYZ.ZERO.copy(y = movementStep) },
        Key.D to { world.offset += XYZ.ZERO.copy(x = movementStep) },
        Key.DirectionUp to { world.offset += XYZ.ZERO.copy(z = movementStep) },
        Key.DirectionDown to { world.offset += XYZ.ZERO.copy(z = -movementStep) },
        Key.R to { world.scale *= XYZ.ONE.copy(x = 1 + scaleStep) },
        Key.T to { world.scale *= XYZ.ONE.copy(x = 1 - scaleStep) },
        Key.F to { world.scale *= XYZ.ONE.copy(y = 1 + scaleStep) },
        Key.G to { world.scale *= XYZ.ONE.copy(y = 1 - scaleStep) },
        Key.Y to { world.scale *= XYZ.ONE.copy(z = 1 + scaleStep) },
        Key.U to { world.scale *= XYZ.ONE.copy(z = 1 - scaleStep) },
        Key.Q to { world.xyRadians += rotationStep },
        Key.E to { world.xyRadians -= rotationStep },
        Key.Z to { world.yzRadians += rotationStep },
        Key.X to { world.yzRadians -= rotationStep },
        Key.C to { world.zxRadians += rotationStep },
        Key.V to { world.zxRadians -= rotationStep }
    )

    fun integrateIntoKeysFlow(
        observeKeysPressed: (predicate: (KeyEvent) -> Boolean, action: (KeyEvent) -> Unit) -> Unit
    ) {
        for ((key, action) in overrides) {
            observeKeysPressed({it.key == key}, { action() })
        }
    }

    private fun setWorldRotation(
        xy: Number = world.xyRadians,
        yz: Number = world.yzRadians,
        zx: Number = world.zxRadians
    ) {
        world.xyRadians = xy.toFloat()
        world.yzRadians = yz.toFloat()
        world.zxRadians = zx.toFloat()
    }
}
