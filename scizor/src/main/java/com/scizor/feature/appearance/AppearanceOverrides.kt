package com.scizor.feature.appearance

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import com.scizor.core.ScizorStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class AppearanceMode { SYSTEM, LIGHT, DARK }

/**
 * Forces the host app's light/dark appearance via [UiModeManager.setApplicationNightMode]
 * (Android 12+). The choice persists via the Scizor store.
 */
internal object AppearanceOverrides {

    private var store: ScizorStore? = null
    private var appContext: Context? = null

    private val _mode = MutableStateFlow(AppearanceMode.SYSTEM)
    val mode: StateFlow<AppearanceMode> = _mode.asStateFlow()

    fun init(context: Context, store: ScizorStore) {
        this.store = store
        this.appContext = context.applicationContext
        _mode.value = runCatching {
            AppearanceMode.valueOf(store.string("appearance_mode") ?: AppearanceMode.SYSTEM.name)
        }.getOrDefault(AppearanceMode.SYSTEM)
        apply(_mode.value)
    }

    fun set(mode: AppearanceMode) {
        _mode.value = mode
        store?.putString("appearance_mode", mode.name)
        apply(mode)
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
