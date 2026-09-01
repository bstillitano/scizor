package com.scizor.ui

import androidx.compose.runtime.Composable
import com.scizor.feature.custom.ScizorIcon
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
        val icon: ScizorIcon? = null,
    ) : MenuRow

    /** A navigable feature or a runnable developer option. */
    data class Action(
        override val id: String,
        val title: String,
        val subtitle: String?,
        val icon: ScizorIcon,
        val action: MenuAction,
        /** The registry id this row can be pinned/unpinned by, or null if not pinnable. */
        val pinnableId: String? = null,
    ) : MenuRow

    /**
     * An inline on/off switch.
     *
     * Exactly one of [flow] and [checked] is non-null. Built-in toggles supply a
     * [StateFlow]; host-contributed ones supply a lambda, so the host's own store
     * stays the source of truth and the row re-reads it on recomposition.
     */
    data class Toggle(
        override val id: String,
        val title: String,
        val subtitle: String?,
        val icon: ScizorIcon,
        val flow: StateFlow<Boolean>? = null,
        val checked: (() -> Boolean)? = null,
        val onChange: (Boolean) -> Unit,
    ) : MenuRow
}

internal data class MenuGroupUi(
    val title: String,
    val rows: List<MenuRow>,
)
