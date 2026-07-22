package com.scizor.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.StateFlow

/** What happens when a menu item is tapped. */
internal sealed interface MenuAction {
    /** Navigate to a feature [screen] (which receives the navigator for child pages). */
    data class Open(
        val title: String,
        val screen: @Composable (ScizorNavigator) -> Unit,
    ) : MenuAction

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
        val icon: ImageVector? = null,
    ) : MenuRow

    /** A navigable feature or a runnable developer option. */
    data class Action(
        override val id: String,
        val title: String,
        val subtitle: String?,
        val icon: ImageVector,
        val action: MenuAction,
        /** The registry id this row can be pinned/unpinned by, or null if not pinnable. */
        val pinnableId: String? = null,
    ) : MenuRow

    /** An inline on/off switch (quick UI-tool toggles surfaced in the menu). */
    data class Toggle(
        override val id: String,
        val title: String,
        val subtitle: String?,
        val icon: ImageVector,
        val flow: StateFlow<Boolean>,
        val onChange: (Boolean) -> Unit,
    ) : MenuRow
}

internal data class MenuGroupUi(
    val title: String,
    val rows: List<MenuRow>,
)
