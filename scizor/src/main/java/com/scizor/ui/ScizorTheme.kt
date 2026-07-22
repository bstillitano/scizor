package com.scizor.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Branded fallback for devices without dynamic color (pre-Android 12).
private val FallbackLight = lightColorScheme()
private val FallbackDark = darkColorScheme()

/**
 * Material 3 Expressive theme for all Scizor UI. Uses [MaterialExpressiveTheme]
 * for the expressive type, shape, and motion defaults, with Material You dynamic
 * color on Android 12+, following the host device's dark/light setting.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
