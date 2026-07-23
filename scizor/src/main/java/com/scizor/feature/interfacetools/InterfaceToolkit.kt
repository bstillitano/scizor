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

    /** Grid line colour. */
    enum class GridColor { RED, GREEN, BLUE }

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
    private val _gridOpacity = MutableStateFlow(50) // percent
    private val _gridColor = MutableStateFlow(GridColor.RED)
    val gridSizeDp: StateFlow<Int> = _gridSizeDp
    val gridOpacity: StateFlow<Int> = _gridOpacity
    val gridColor: StateFlow<GridColor> = _gridColor

    // FPS settings
    private val _fpsCorner = MutableStateFlow(Corner.TOP_LEFT)
    private val _currentFps = MutableStateFlow(0)
    val fpsCorner: StateFlow<Corner> = _fpsCorner

    /** The live frame rate, published by the overlay for the settings status readout. */
    val currentFps: StateFlow<Int> = _currentFps

    // Touch settings — toggles, matching Scyther.
    private val _showTouchDuration = MutableStateFlow(false)
    private val _showTouchRadius = MutableStateFlow(false)
    private val _touchLogging = MutableStateFlow(false)
    val showTouchDuration: StateFlow<Boolean> = _showTouchDuration
    val showTouchRadius: StateFlow<Boolean> = _showTouchRadius
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
        _gridOpacity.value = int("ui_grid_opacity", 50)
        _gridColor.value = GridColor.entries.getOrElse(int("ui_grid_color", 0)) { GridColor.RED }
        _fpsCorner.value = Corner.entries.getOrElse(int("ui_fps_corner", 0)) { Corner.TOP_LEFT }
        _showTouchDuration.value = store.boolean("ui_touch_duration", false)
        _showTouchRadius.value = store.boolean("ui_touch_radius_on", false)
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

    fun setGridSizeDp(value: Int) = updateInt(_gridSizeDp, "ui_grid_size", value.coerceIn(1, 100))
    fun setGridOpacity(value: Int) = updateInt(_gridOpacity, "ui_grid_opacity", value.coerceIn(1, 100))
    fun setGridColor(value: GridColor) {
        _gridColor.value = value
        store?.putString("ui_grid_color", value.ordinal.toString())
    }
    fun setFpsCorner(value: Corner) {
        _fpsCorner.value = value
        store?.putString("ui_fps_corner", value.ordinal.toString())
    }

    /** Publishes the current frame rate from the overlay. */
    fun reportFps(fps: Int) {
        if (_currentFps.value != fps) _currentFps.value = fps
    }

    fun setShowTouchDuration(value: Boolean) = update(_showTouchDuration, "ui_touch_duration", value)
    fun setShowTouchRadius(value: Boolean) = update(_showTouchRadius, "ui_touch_radius_on", value)
    fun setTouchLogging(value: Boolean) = update(_touchLogging, "ui_touch_log", value)

    /** Called by the overlay when a touch is captured and logging is enabled. */
    fun logTouch(x: Float, y: Float) {
        if (!_touchLogging.value) return
        _touchLog.value = (listOf("(%.0f, %.0f)".format(x, y)) + _touchLog.value).take(100)
    }

    fun clearTouchLog() {
        _touchLog.value = emptyList()
    }

    /** Invoked whenever an overlay on/off toggle changes, so the host can add/remove the overlay window. */
    var onOverlayChanged: (() -> Unit)? = null

    private fun update(flow: MutableStateFlow<Boolean>, key: String, value: Boolean) {
        flow.value = value
        store?.putBoolean(key, value)
        onOverlayChanged?.invoke()
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
