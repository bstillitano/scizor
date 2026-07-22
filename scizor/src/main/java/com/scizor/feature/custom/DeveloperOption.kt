package com.scizor.feature.custom

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A custom entry the host app adds to the Scizor menu. Mirrors Scyther's three
 * option types:
 * - set [value] for a read-only value row,
 * - set [screen] to push a Composable when tapped,
 * - otherwise [onClick] runs an action.
 */
data class DeveloperOption(
    val title: String,
    val icon: ImageVector? = null,
    val value: String? = null,
    val screen: (@Composable () -> Unit)? = null,
    val onClick: () -> Unit = {},
)
