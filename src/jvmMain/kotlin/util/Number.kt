package util

import components.TransformationResult

fun transformStringToFloat(string: String) = try {
    TransformationResult.Success(string.toFloat() % 360)
} catch (e: NumberFormatException) {
    TransformationResult.FailureMessage("Bad number format")
}