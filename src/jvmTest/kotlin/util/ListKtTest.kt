package util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ListKtTest {

    @Test
    fun `test dropAt with 0th index`() {
        assertEquals(
            listOf(1, 2, 3),
            listOf(0, 1, 2, 3).dropAt(0)
        )
    }

    @Test
    fun `test dropAt with regular index`() {
        assertEquals(
            listOf(0, 2, 3),
            listOf(0, 1, 2, 3).dropAt(1)
        )
    }

    @Test
    fun `test dropAt with last index`() {
        assertEquals(
            listOf(0, 1, 2),
            listOf(0, 1, 2, 3).run { dropAt(lastIndex) }
        )
    }

    @Test
    fun combinationsOfPairs() {
        assertEquals(listOf(1 to 2), listOf(1, 2).combinationsOfPairs())
        assertEquals(listOf(1 to 2, 1 to 3, 2 to 3), listOf(1, 2, 3).combinationsOfPairs())
        assertEquals(listOf(1 to 2, 1 to 3, 1 to 4, 2 to 3, 2 to 4, 3 to 4), listOf(1, 2, 3, 4).combinationsOfPairs())
    }
}
