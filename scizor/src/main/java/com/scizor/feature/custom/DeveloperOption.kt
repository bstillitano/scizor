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
     *
     * [checked] is called during composition and re-read after every one, so it
     * must be a stable, cheap, side-effect-free read of the host's own store —
     * back-to-back calls must return the same value. Do not derive it from a
     * clock, randomness, or anything else that changes per invocation; a lambda
     * that does will keep this row recomposing for as long as the menu is open.
     *
     * [onCheckedChange] is assumed synchronous with [checked]: the row writes
     * through and then immediately re-reads [checked] to display what the host
     * actually stored. A host that persists asynchronously will briefly show the
     * old value.
     */
    data class Toggle(
        override val title: String,
        val checked: () -> Boolean,
        val onCheckedChange: (Boolean) -> Unit,
        override val subtitle: String? = null,
        override val icon: ScizorIcon? = null,
    ) : DeveloperOption
}
