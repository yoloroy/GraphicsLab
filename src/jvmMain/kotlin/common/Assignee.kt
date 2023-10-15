package common

import XYZ
import androidx.compose.ui.unit.IntSize
import components.Assignee
import util.transformSpaceDelimitedStringToIntSize
import util.transformSpaceDelimitedStringToXYZ
import util.transformStringToFloat

fun Assignee.Companion.forXYZ(name: String, assign: (XYZ) -> Unit, startValue: String): Assignee<XYZ> {
    return Assignee(
        name,
        ::transformSpaceDelimitedStringToXYZ,
        assign,
        startValue
    )
}

fun Assignee.Companion.forIntSize(name: String, assign: (IntSize) -> Unit, startValue: String): Assignee<IntSize> {
    return Assignee(
        name,
        ::transformSpaceDelimitedStringToIntSize,
        assign,
        startValue
    )
}

fun Assignee.Companion.forFloat(name: String, assign: (Float) -> Unit, startValue: String): Assignee<Float> {
    return Assignee(
        name,
        ::transformStringToFloat,
        assign,
        startValue
    )
}
