package com.scizor.feature.custom

import androidx.compose.runtime.Composable

/**
 * A custom entry the host app adds to the Scizor menu's Development Tools
 * section. Mirrors Scyther's option types, plus a toggle.
 */
sealed interface DeveloperOption {

    val title: String
    val subtitle: String?
    val icon: ScizorIcon?

    /** A read-only label and value. */
    data class Value(
        override val title: String,
        val value: String,
        override val subtitle: String? = null,
        override val icon: ScizorIcon? = null,
    ) : DeveloperOption

    /** Pushes [screen] onto the menu's navigation stack when tapped. */
    data class Screen(
        override val title: String,
        val screen: @Composable () -> Unit,
        override val subtitle: String? = null,
        override val icon: ScizorIcon? = null,
    ) : DeveloperOption

    /**
     * Runs [onClick] when tapped.
     *
     * Set [dismissOnClick] for an action that navigates into the host app — a
     * deep link, a screen, a permission prompt — so the menu gets out of the way
     * first, without every host writing `Scizor.dismiss()` as the first line of
     * every action.
     */
    data class Action(
        override val title: String,
        override val subtitle: String? = null,
        override val icon: ScizorIcon? = null,
        val dismissOnClick: Boolean = false,
        val onClick: () -> Unit,
    ) : DeveloperOption

    /**
     * An on/off switch backed by the host's own storage.
     *
     * [checked] is a lambda rather than a `Boolean` so the host stays the source
     * of truth: the row reads it whenever the menu recomposes, instead of
     * snapshotting the value at registration time.
     */
    data class Toggle(
        override val title: String,
        val checked: () -> Boolean,
        val onCheckedChange: (Boolean) -> Unit,
        override val subtitle: String? = null,
        override val icon: ScizorIcon? = null,
    ) : DeveloperOption
}
