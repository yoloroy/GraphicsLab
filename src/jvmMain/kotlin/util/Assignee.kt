package util

import XYZ
import components.Assignee

fun Assignee.Companion.forXYZ(name: String, assign: (XYZ) -> Unit, startValue: String): Assignee<XYZ> {
    return Assignee(
        name,
        ::transformSpaceDelimitedStringToXYZ,
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
