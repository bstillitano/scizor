package com.scizor.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/** What happens when a menu item is tapped. */
internal sealed interface MenuAction {
    /** Navigate to a feature [screen]. */
    data class Open(val title: String, val screen: @Composable () -> Unit) : MenuAction

    /** Run an arbitrary [block] (used by custom developer options). */
    data class Run(val block: () -> Unit) : MenuAction
}

internal data class MenuItemUi(
    val id: String,
    val title: String,
    val subtitle: String?,
    val icon: ImageVector,
    val action: MenuAction,
)

internal data class MenuGroupUi(
    val title: String,
    val items: List<MenuItemUi>,
)
