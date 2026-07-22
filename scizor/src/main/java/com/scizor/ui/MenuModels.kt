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

/** A single row within a grouped menu card. */
internal sealed interface MenuRow {
    val id: String

    /** A read-only label/value pair, shown inline (device & app facts). */
    data class Info(
        override val id: String,
        val label: String,
        val value: String,
    ) : MenuRow

    /** A navigable feature or a runnable developer option. */
    data class Action(
        override val id: String,
        val title: String,
        val subtitle: String?,
        val icon: ImageVector,
        val action: MenuAction,
    ) : MenuRow
}

internal data class MenuGroupUi(
    val title: String,
    val rows: List<MenuRow>,
)
