package com.scizor.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Accent matches Scyther's iOS system blue.
private val ScizorBlue = Color(0xFF007AFF)
private val ScizorBlueDark = Color(0xFF0A84FF)

// iOS-style "systemGrouped" surfaces: a tinted background with white/dark cards.
private val LightColors = lightColorScheme(
    primary = ScizorBlue,
    onPrimary = Color.White,
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF8E8E93),
    outlineVariant = Color(0xFFC6C6C8),
)

private val DarkColors = darkColorScheme(
    primary = ScizorBlueDark,
    onPrimary = Color.White,
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFF8E8E93),
    outlineVariant = Color(0xFF38383A),
)

/**
 * Material 3 theme for all Scizor UI. Follows the host device's dark/light
 * setting and mirrors Scyther's grouped-inset visual language.
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
