package util

import XYZ
import components.TransformationResult

fun XYZ.Companion.fromSpaceDelimitedString(string: String) = string
    .trim()
    .split(" ")
    .map { it.toFloat() }
    .let { XYZ(it[0], it[1], it[2]) }

fun transformSpaceDelimitedStringToXYZ(string: String): TransformationResult<XYZ> = try {
    TransformationResult.Success(XYZ.fromSpaceDelimitedString(string))
} catch (e: NumberFormatException) {
    TransformationResult.FailureMessage("Bad number format")
} catch (e: IndexOutOfBoundsException) {
    TransformationResult.FailureMessage("Not enough numbers are provided")
}