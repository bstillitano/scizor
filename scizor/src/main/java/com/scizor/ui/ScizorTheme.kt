package com.scizor.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.scizor.feature.appearance.AppearanceOverrides

// Branded fallback for devices without dynamic color (pre-Android 12).
private val FallbackLight = lightColorScheme()
private val FallbackDark = darkColorScheme()

/**
 * Material 3 Expressive theme for all Scizor UI. Uses Material You dynamic color on
 * Android 12+, following the host device's dark/light setting, and applies the
 * Appearance overrides live: the font-scale multiplier via [LocalDensity] and the
 * high-contrast flag via a maximum-contrast colour scheme.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ScizorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val fontScale by AppearanceOverrides.fontScale.collectAsState()
    val highContrast by AppearanceOverrides.highContrast.collectAsState()

    val colorScheme = when {
        highContrast -> if (darkTheme) HighContrastDark else HighContrastLight
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> FallbackDark
        else -> FallbackLight
    }

    val density = LocalDensity.current
    CompositionLocalProvider(
        // Multiply the device's own font scale by the Appearance override.
        LocalDensity provides Density(density.density, density.fontScale * fontScale),
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

private val HighContrastDark: ColorScheme = darkColorScheme(
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceContainer = Color(0xFF1C1C1C),
    surfaceContainerHigh = Color(0xFF2B2B2B),
    onSurfaceVariant = Color(0xFFE6E6E6),
    primary = Color(0xFFA9C9FF),
    onPrimary = Color.Black,
    outline = Color.White,
    outlineVariant = Color(0xFF8A8A8A),
    error = Color(0xFFFF8A80),
)

private val HighContrastLight: ColorScheme = lightColorScheme(
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceContainer = Color(0xFFEDEDED),
    surfaceContainerHigh = Color(0xFFDCDCDC),
    onSurfaceVariant = Color(0xFF1A1A1A),
    primary = Color(0xFF0B4EC4),
    onPrimary = Color.White,
    outline = Color.Black,
    outlineVariant = Color(0xFF5A5A5A),
    error = Color(0xFFB3261E),
)
