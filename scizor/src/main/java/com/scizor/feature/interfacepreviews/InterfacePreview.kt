package com.scizor.feature.interfacepreviews

import androidx.compose.runtime.Composable

/**
 * A named Composable the host app registers for preview in the menu.
 *
 * Assign a list of these to [com.scizor.Scizor.interfacePreviews].
 */
data class InterfacePreview(
    val name: String,
    val content: @Composable () -> Unit,
)
