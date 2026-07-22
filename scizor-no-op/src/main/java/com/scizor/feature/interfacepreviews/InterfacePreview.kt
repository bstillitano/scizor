package com.scizor.feature.interfacepreviews

import androidx.compose.runtime.Composable

/** No-op mirror of the real [InterfacePreview]. */
data class InterfacePreview(
    val name: String,
    val content: @Composable () -> Unit,
)
