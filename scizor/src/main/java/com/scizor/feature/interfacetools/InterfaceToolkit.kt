package com.scizor.feature.interfacetools

import android.animation.ValueAnimator
import com.scizor.core.ScizorStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the on/off state for the UI overlay tools (grid, view bounds, touch
 * visualiser, FPS counter) and the slow-animations override. State persists via
 * the Scizor store and is read by the per-activity overlay.
 */
internal object InterfaceToolkit {

    private var store: ScizorStore? = null

    private val _grid = MutableStateFlow(false)
    private val _viewBounds = MutableStateFlow(false)
    private val _touches = MutableStateFlow(false)
    private val _fps = MutableStateFlow(false)
    private val _slowAnimations = MutableStateFlow(false)

    val grid: StateFlow<Boolean> = _grid
    val viewBounds: StateFlow<Boolean> = _viewBounds
    val touches: StateFlow<Boolean> = _touches
    val fps: StateFlow<Boolean> = _fps
    val slowAnimations: StateFlow<Boolean> = _slowAnimations

    val anyOverlayActive: Boolean
        get() = _grid.value || _viewBounds.value || _touches.value || _fps.value

    fun init(store: ScizorStore) {
        this.store = store
        _grid.value = store.boolean("ui_grid", false)
        _viewBounds.value = store.boolean("ui_bounds", false)
        _touches.value = store.boolean("ui_touches", false)
        _fps.value = store.boolean("ui_fps", false)
        _slowAnimations.value = store.boolean("ui_slow", false)
        applySlowAnimations(_slowAnimations.value)
    }

    fun setGrid(value: Boolean) = update(_grid, "ui_grid", value)
    fun setViewBounds(value: Boolean) = update(_viewBounds, "ui_bounds", value)
    fun setTouches(value: Boolean) = update(_touches, "ui_touches", value)
    fun setFps(value: Boolean) = update(_fps, "ui_fps", value)

    fun setSlowAnimations(value: Boolean) {
        update(_slowAnimations, "ui_slow", value)
        applySlowAnimations(value)
    }

    private fun update(flow: MutableStateFlow<Boolean>, key: String, value: Boolean) {
        flow.value = value
        store?.putBoolean(key, value)
    }

    private fun applySlowAnimations(enabled: Boolean) {
        runCatching {
            val method = ValueAnimator::class.java
                .getMethod("setDurationScale", Float::class.javaPrimitiveType)
            method.invoke(null, if (enabled) 10f else 1f)
        }
    }
}
