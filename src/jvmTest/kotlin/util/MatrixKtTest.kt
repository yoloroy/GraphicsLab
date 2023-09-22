package util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MatrixKtTest {

    @Test
    fun times() {
        assertEquals(
            listOf(1F),
            (listOf(listOf(1F)) * listOf(listOf(1F))).flatten()
        )

        assertEquals(
            listOf(
                listOf(140F, 146F),
                listOf(320F, 335F)
            ).flatten(),
            (listOf(
                listOf(1F, 2F, 3F),
                listOf(4F, 5F, 6F)
            ) * listOf(
                listOf(10F, 11F),
                listOf(20F, 21F),
                listOf(30F, 31F)
            )).flatten()
        )
    }
}
