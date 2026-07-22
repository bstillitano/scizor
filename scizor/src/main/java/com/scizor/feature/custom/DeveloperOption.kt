package com.scizor.feature.custom

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A custom entry the host app adds to the Scizor menu.
 *
 * Assign a list of these to [com.scizor.Scizor.developerOptions].
 */
data class DeveloperOption(
    val title: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit,
)
