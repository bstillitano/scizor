package com.scizor.feature.custom

import androidx.compose.runtime.Composable

/** No-op mirror of the real [DeveloperOption]. */
sealed interface DeveloperOption {

    val title: String
    val subtitle: String?
    val icon: ScizorIcon?

    data class Value(
        override val title: String,
        val value: String,
        override val subtitle: String? = null,
        override val icon: ScizorIcon? = null,
    ) : DeveloperOption

    data class Screen(
        override val title: String,
        val screen: @Composable () -> Unit,
        override val subtitle: String? = null,
        override val icon: ScizorIcon? = null,
    ) : DeveloperOption

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
