package util

fun IntRange.toIntArray(): IntArray = IntArray(count()).apply array@ {
    for (i in indices) {
        this@array[i] = this@toIntArray.elementAt(i)
    }
}
