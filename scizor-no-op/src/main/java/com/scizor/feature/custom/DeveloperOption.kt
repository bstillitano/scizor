package com.scizor.feature.custom

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/** No-op mirror of the real [DeveloperOption]. */
data class DeveloperOption(
    val title: String,
    val icon: ImageVector? = null,
    val value: String? = null,
    val screen: (@Composable () -> Unit)? = null,
    val onClick: () -> Unit = {},
)
