package components

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.MenuScope
import input.PointsGeneralActions

context(MenuScope)
@Composable
fun PointsGeneralActions.MenuBarItems() {
    Item(text = "Copy", onClick = ::copy)
    Item(text = "Paste", onClick = ::paste)
    Item(text = "Clear", onClick = ::clear)
}