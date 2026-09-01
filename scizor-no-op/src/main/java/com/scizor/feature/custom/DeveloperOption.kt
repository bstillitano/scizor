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

    data class Toggle(
        override val title: String,
        val checked: () -> Boolean,
        val onCheckedChange: (Boolean) -> Unit,
        override val subtitle: String? = null,
        override val icon: ScizorIcon? = null,
    ) : DeveloperOption
}
