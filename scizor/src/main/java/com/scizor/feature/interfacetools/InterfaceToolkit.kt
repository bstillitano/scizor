package com.scizor.feature.interfacetools

import android.animation.ValueAnimator
import com.scizor.core.ScizorStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the on/off state and per-tool settings for the UI overlay tools (grid,
 * view frames, view sizes, touch visualiser, FPS counter) and the slow-animations
 * override. State persists via the Scizor store and is read by the per-activity
 * overlay. Integer settings are stored as strings since the store is boolean/string.
 */
internal object InterfaceToolkit {

    /** Where the FPS counter is anchored. */
    enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    private var store: ScizorStore? = null

    // On/off toggles
    private val _grid = MutableStateFlow(false)
    private val _frames = MutableStateFlow(false)
    private val _sizes = MutableStateFlow(false)
    private val _touches = MutableStateFlow(false)
    private val _fps = MutableStateFlow(false)
    private val _slowAnimations = MutableStateFlow(false)

    val grid: StateFlow<Boolean> = _grid
    val frames: StateFlow<Boolean> = _frames
    val sizes: StateFlow<Boolean> = _sizes
    val touches: StateFlow<Boolean> = _touches
    val fps: StateFlow<Boolean> = _fps
    val slowAnimations: StateFlow<Boolean> = _slowAnimations

    // Grid settings
    private val _gridSizeDp = MutableStateFlow(8)
    private val _gridOpacity = MutableStateFlow(13) // percent
    val gridSizeDp: StateFlow<Int> = _gridSizeDp
    val gridOpacity: StateFlow<Int> = _gridOpacity

    // FPS settings
    private val _fpsCorner = MutableStateFlow(Corner.TOP_LEFT)
    private val _fpsWarn = MutableStateFlow(45)
    private val _fpsCritical = MutableStateFlow(30)
    private val _fpsAveraged = MutableStateFlow(true)
    val fpsCorner: StateFlow<Corner> = _fpsCorner
    val fpsWarn: StateFlow<Int> = _fpsWarn
    val fpsCritical: StateFlow<Int> = _fpsCritical
    val fpsAveraged: StateFlow<Boolean> = _fpsAveraged

    // Touch settings
    private val _touchRadiusDp = MutableStateFlow(12)
    private val _touchFadeMs = MutableStateFlow(800)
    private val _touchLogging = MutableStateFlow(false)
    val touchRadiusDp: StateFlow<Int> = _touchRadiusDp
    val touchFadeMs: StateFlow<Int> = _touchFadeMs
    val touchLogging: StateFlow<Boolean> = _touchLogging

    private val _touchLog = MutableStateFlow<List<String>>(emptyList())
    val touchLog: StateFlow<List<String>> = _touchLog

    val anyOverlayActive: Boolean
        get() = _grid.value || _frames.value || _sizes.value || _touches.value || _fps.value

    fun init(store: ScizorStore) {
        this.store = store
        _grid.value = store.boolean("ui_grid", false)
        _frames.value = store.boolean("ui_bounds", false)
        _sizes.value = store.boolean("ui_sizes", false)
        _touches.value = store.boolean("ui_touches", false)
        _fps.value = store.boolean("ui_fps", false)
        _slowAnimations.value = store.boolean("ui_slow", false)
        _gridSizeDp.value = int("ui_grid_size", 8)
        _gridOpacity.value = int("ui_grid_opacity", 13)
        _fpsCorner.value = Corner.entries.getOrElse(int("ui_fps_corner", 0)) { Corner.TOP_LEFT }
        _fpsWarn.value = int("ui_fps_warn", 45)
        _fpsCritical.value = int("ui_fps_critical", 30)
        _fpsAveraged.value = store.boolean("ui_fps_avg", true)
        _touchRadiusDp.value = int("ui_touch_radius", 12)
        _touchFadeMs.value = int("ui_touch_fade", 800)
        _touchLogging.value = store.boolean("ui_touch_log", false)
        applySlowAnimations(_slowAnimations.value)
    }

    fun setGrid(value: Boolean) = update(_grid, "ui_grid", value)
    fun setFrames(value: Boolean) = update(_frames, "ui_bounds", value)
    fun setSizes(value: Boolean) = update(_sizes, "ui_sizes", value)
    fun setTouches(value: Boolean) = update(_touches, "ui_touches", value)
    fun setFps(value: Boolean) = update(_fps, "ui_fps", value)

    fun setSlowAnimations(value: Boolean) {
        update(_slowAnimations, "ui_slow", value)
        applySlowAnimations(value)
    }

    fun setGridSizeDp(value: Int) = updateInt(_gridSizeDp, "ui_grid_size", value.coerceIn(2, 32))
    fun setGridOpacity(value: Int) = updateInt(_gridOpacity, "ui_grid_opacity", value.coerceIn(2, 100))
    fun setFpsCorner(value: Corner) {
        _fpsCorner.value = value
        store?.putString("ui_fps_corner", value.ordinal.toString())
    }
    fun setFpsWarn(value: Int) = updateInt(_fpsWarn, "ui_fps_warn", value.coerceIn(1, 120))
    fun setFpsCritical(value: Int) = updateInt(_fpsCritical, "ui_fps_critical", value.coerceIn(1, 120))
    fun setFpsAveraged(value: Boolean) = update(_fpsAveraged, "ui_fps_avg", value)
    fun setTouchRadiusDp(value: Int) = updateInt(_touchRadiusDp, "ui_touch_radius", value.coerceIn(4, 48))
    fun setTouchFadeMs(value: Int) = updateInt(_touchFadeMs, "ui_touch_fade", value.coerceIn(200, 3000))
    fun setTouchLogging(value: Boolean) = update(_touchLogging, "ui_touch_log", value)

    /** Called by the overlay when a touch is captured and logging is enabled. */
    fun logTouch(x: Float, y: Float) {
        if (!_touchLogging.value) return
        _touchLog.value = (listOf("(%.0f, %.0f)".format(x, y)) + _touchLog.value).take(100)
    }

    fun clearTouchLog() {
        _touchLog.value = emptyList()
    }

    private fun update(flow: MutableStateFlow<Boolean>, key: String, value: Boolean) {
        flow.value = value
        store?.putBoolean(key, value)
    }

    private fun updateInt(flow: MutableStateFlow<Int>, key: String, value: Int) {
        flow.value = value
        store?.putString(key, value.toString())
    }

    private fun int(key: String, default: Int): Int = store?.string(key)?.toIntOrNull() ?: default

    private fun applySlowAnimations(enabled: Boolean) {
        runCatching {
            val method = ValueAnimator::class.java
                .getMethod("setDurationScale", Float::class.javaPrimitiveType)
            method.invoke(null, if (enabled) 10f else 1f)
        }
    }
}
