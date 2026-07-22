package com.scizor.feature.appearance

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.scizor.core.ScizorStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class AppearanceMode { SYSTEM, LIGHT, DARK }

/**
 * Forces the host app's appearance:
 *  - light/dark via [UiModeManager.setApplicationNightMode] (Android 12+),
 *  - a font-scale multiplier applied through [wrap] (host calls it in
 *    `Activity.attachBaseContext`),
 *  - a high-contrast flag hosts can read.
 * All choices persist via the Scizor store.
 */
internal object AppearanceOverrides {

    const val DEFAULT_FONT_SCALE = 1.0f

    private var store: ScizorStore? = null
    private var appContext: Context? = null

    private val _mode = MutableStateFlow(AppearanceMode.SYSTEM)
    val mode: StateFlow<AppearanceMode> = _mode.asStateFlow()

    private val _fontScale = MutableStateFlow(DEFAULT_FONT_SCALE)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    private val _highContrast = MutableStateFlow(false)
    val highContrast: StateFlow<Boolean> = _highContrast.asStateFlow()

    /** The device's current system font scale, for reference in the UI. */
    fun systemFontScale(): Float =
        appContext?.resources?.configuration?.fontScale ?: DEFAULT_FONT_SCALE

    fun init(context: Context, store: ScizorStore) {
        this.store = store
        this.appContext = context.applicationContext
        _mode.value = runCatching {
            AppearanceMode.valueOf(store.string("appearance_mode") ?: AppearanceMode.SYSTEM.name)
        }.getOrDefault(AppearanceMode.SYSTEM)
        _fontScale.value = store.string("appearance_font_scale")?.toFloatOrNull() ?: DEFAULT_FONT_SCALE
        _highContrast.value = store.boolean("appearance_high_contrast", false)
        apply(_mode.value)
    }

    fun set(mode: AppearanceMode) {
        _mode.value = mode
        store?.putString("appearance_mode", mode.name)
        apply(mode)
    }

    fun setFontScale(scale: Float) {
        val clamped = scale.coerceIn(0.85f, 1.5f)
        _fontScale.value = clamped
        store?.putString("appearance_font_scale", clamped.toString())
    }

    fun setHighContrast(value: Boolean) {
        _highContrast.value = value
        store?.putBoolean("appearance_high_contrast", value)
    }

    fun reset() {
        set(AppearanceMode.SYSTEM)
        setFontScale(DEFAULT_FONT_SCALE)
        setHighContrast(false)
    }

    /** Wraps [base] with the font-scale override applied. Hosts call this in `attachBaseContext`. */
    fun wrap(base: Context): Context {
        if (_fontScale.value == DEFAULT_FONT_SCALE) return base
        val config = Configuration(base.resources.configuration)
        config.fontScale = _fontScale.value
        return base.createConfigurationContext(config)
    }

    private fun apply(mode: AppearanceMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val uiModeManager = appContext?.getSystemService(UiModeManager::class.java) ?: return
        val night = when (mode) {
            AppearanceMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
            AppearanceMode.DARK -> UiModeManager.MODE_NIGHT_YES
            AppearanceMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
        }
        runCatching { uiModeManager.setApplicationNightMode(night) }
    }
}
