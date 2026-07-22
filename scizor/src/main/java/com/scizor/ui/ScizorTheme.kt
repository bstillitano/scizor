package com.scizor.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Branded fallback seed for devices without dynamic color (pre-Android 12).
private val FallbackLight = lightColorScheme()
private val FallbackDark = darkColorScheme()

// Expressive shape scale — generous rounding is a hallmark of Material 3 Expressive.
private val ScizorShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Material 3 (Expressive) theme for all Scizor UI. Uses Material You dynamic
 * color on Android 12+, falling back to the M3 baseline scheme, and follows the
 * host device's dark/light setting.
 */
@Composable
internal fun ScizorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> FallbackDark
        else -> FallbackLight
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = ScizorShapes,
        content = content,
    )
}
