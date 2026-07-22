package com.scizor.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ScizorRed = Color(0xFFD23B3B)
private val ScizorRedDark = Color(0xFFE85D5D)

private val LightColors = lightColorScheme(
    primary = ScizorRed,
    secondary = Color(0xFF4A4A4A),
)

private val DarkColors = darkColorScheme(
    primary = ScizorRedDark,
    secondary = Color(0xFFB0B0B0),
)

/**
 * Material 3 theme for all Scizor UI. Follows the host device's dark/light
 * setting so the debug menu blends with the environment it is launched over.
 */
@Composable
internal fun ScizorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
