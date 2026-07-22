package com.scizor.feature.interfacetools

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

/**
 * A pass-through overlay added on top of each Activity's content. Draws the
 * active interface tools (grid, view frames, view sizes, touch visualiser, FPS).
 *
 * The touch visualiser tracks every active pointer's live position — fed from the
 * window's `dispatchTouchEvent` via [feedTouch] so it follows dragging fingers
 * rather than only marking taps. The view itself never consumes touches.
 */
internal class ScizorOverlayView(context: Context) : View(context) {

    private data class Touch(val x: Float, val y: Float, val at: Long)

    /** Live position of each active pointer, keyed by pointer id — follows the finger. */
    private val activePointers = HashMap<Int, PointF>()

    /** Fading marks left behind when a pointer lifts. */
    private val trails = ArrayDeque<Touch>()

    private var lastFrameNanos = 0L
    private var currentFps = 0
    private val fpsHistory = ArrayDeque<Int>()

    private val density = resources.displayMetrics.density

    private val gridPaint = Paint().apply { strokeWidth = 1f }
    private val boundsPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = 0x8800B0FF.toInt()
        strokeWidth = 1.5f * density
    }
    private val sizeLabelBg = Paint().apply { color = 0xCC0091EA.toInt() }
    private val sizeLabelText = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 9f * density
        isAntiAlias = true
    }
    private val touchPaint = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val touchRingPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeWidth = 2f * density
    }
    private val fpsBg = Paint().apply { color = 0xCC000000.toInt() }
    private val fpsText = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 14f * density
        isAntiAlias = true
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (lastFrameNanos != 0L) {
                val delta = frameTimeNanos - lastFrameNanos
                if (delta > 0) {
                    currentFps = (1_000_000_000.0 / delta).toInt().coerceAtMost(240)
                    fpsHistory.addLast(currentFps)
                    if (fpsHistory.size > 60) fpsHistory.removeFirst()
                }
            }
            lastFrameNanos = frameTimeNanos
            if (InterfaceToolkit.anyOverlayActive) invalidate()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    init {
        setWillNotDraw(false)
        isClickable = false
        isFocusable = false
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    /**
     * Observes a touch event without consuming it (called from the window callback).
     * Updates each active pointer's live position so the visualiser tracks drags.
     */
    fun feedTouch(event: MotionEvent) {
        if (!InterfaceToolkit.touches.value) {
            if (activePointers.isNotEmpty()) {
                activePointers.clear()
                invalidate()
            }
            return
        }
        // Window-callback coordinates are window-relative; map into overlay-local space.
        val loc = IntArray(2).also { getLocationInWindow(it) }
        fun x(i: Int) = event.getX(i) - loc[0]
        fun y(i: Int) = event.getY(i) - loc[1]
        val now = System.currentTimeMillis()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = event.actionIndex
                activePointers[event.getPointerId(i)] = PointF(x(i), y(i))
                InterfaceToolkit.logTouch(x(i), y(i))
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    activePointers[id]?.set(x(i), y(i)) ?: run { activePointers[id] = PointF(x(i), y(i)) }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                activePointers.remove(event.getPointerId(event.actionIndex))
                    ?.let { trails.addLast(Touch(it.x, it.y, now)) }
            }
            MotionEvent.ACTION_CANCEL -> {
                activePointers.values.forEach { trails.addLast(Touch(it.x, it.y, now)) }
                activePointers.clear()
            }
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (InterfaceToolkit.grid.value) drawGrid(canvas)
        if (InterfaceToolkit.frames.value || InterfaceToolkit.sizes.value) drawBounds(canvas)
        if (InterfaceToolkit.touches.value) drawTouches(canvas)
        if (InterfaceToolkit.fps.value) drawFps(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        val alpha = (InterfaceToolkit.gridOpacity.value * 255 / 100).coerceIn(0, 255)
        gridPaint.color = (alpha shl 24)
        val step = InterfaceToolkit.gridSizeDp.value * density
        var x = step
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += step
        }
        var y = step
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += step
        }
    }

    private fun drawBounds(canvas: Canvas) {
        val root = parent as? ViewGroup ?: return
        val drawFrames = InterfaceToolkit.frames.value
        val drawSizes = InterfaceToolkit.sizes.value
        val overlayLoc = IntArray(2).also { getLocationInWindow(it) }
        fun visit(view: View) {
            if (view === this) return
            if (view.width > 0 && view.height > 0 && view.visibility == VISIBLE) {
                val loc = IntArray(2).also { view.getLocationInWindow(it) }
                val left = (loc[0] - overlayLoc[0]).toFloat()
                val top = (loc[1] - overlayLoc[1]).toFloat()
                if (drawFrames) {
                    canvas.drawRect(left, top, left + view.width, top + view.height, boundsPaint)
                }
                if (drawSizes) drawSizeLabel(canvas, view, left, top)
            }
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) visit(view.getChildAt(i))
            }
        }
        for (i in 0 until root.childCount) visit(root.getChildAt(i))
    }

    private fun drawSizeLabel(canvas: Canvas, view: View, left: Float, top: Float) {
        val w = (view.width / density).toInt()
        val h = (view.height / density).toInt()
        val label = "${w}×${h}"
        val pad = 2f * density
        val tw = sizeLabelText.measureText(label)
        val th = sizeLabelText.textSize
        canvas.drawRect(left, top, left + tw + pad * 2, top + th + pad * 2, sizeLabelBg)
        canvas.drawText(label, left + pad, top + th + pad, sizeLabelText)
    }

    private fun drawTouches(canvas: Canvas) {
        val fadeMs = InterfaceToolkit.touchFadeMs.value.toLong().coerceAtLeast(1)
        val radius = InterfaceToolkit.touchRadiusDp.value * density
        val now = System.currentTimeMillis()

        // Fading marks for lifted fingers (expand + fade out).
        while (trails.isNotEmpty() && now - trails.first().at > fadeMs) trails.removeFirst()
        trails.forEach { t ->
            val progress = (now - t.at) / fadeMs.toFloat()
            val alpha = (150 * (1f - progress)).toInt().coerceIn(0, 255)
            touchPaint.color = (alpha shl 24) or TOUCH_RGB
            canvas.drawCircle(t.x, t.y, radius * (1f + progress), touchPaint)
        }

        // Active fingers: a solid spot with a ring and crosshair, exactly under each pointer.
        activePointers.values.forEach { p ->
            touchPaint.color = (0x66 shl 24) or TOUCH_RGB
            canvas.drawCircle(p.x, p.y, radius, touchPaint)
            touchRingPaint.color = (0xFF shl 24) or TOUCH_RGB
            canvas.drawCircle(p.x, p.y, radius, touchRingPaint)
            canvas.drawLine(p.x - radius, p.y, p.x + radius, p.y, touchRingPaint)
            canvas.drawLine(p.x, p.y - radius, p.x, p.y + radius, touchRingPaint)
        }
    }

    private fun drawFps(canvas: Canvas) {
        val averaged = InterfaceToolkit.fpsAveraged.value
        val value = if (averaged && fpsHistory.isNotEmpty()) fpsHistory.average().toInt() else currentFps
        val label = "FPS: $value"
        fpsText.color = when {
            value <= InterfaceToolkit.fpsCritical.value -> 0xFFFF5252.toInt()
            value <= InterfaceToolkit.fpsWarn.value -> 0xFFFFC107.toInt()
            else -> 0xFF69F0AE.toInt()
        }
        val pad = 8f * density
        val textWidth = fpsText.measureText(label)
        val boxWidth = textWidth + pad * 2
        val boxHeight = fpsText.textSize + pad * 2
        val margin = 16f * density
        val (boxLeft, boxTop) = when (InterfaceToolkit.fpsCorner.value) {
            InterfaceToolkit.Corner.TOP_LEFT -> margin to margin * 2.5f
            InterfaceToolkit.Corner.TOP_RIGHT -> (width - boxWidth - margin) to margin * 2.5f
            InterfaceToolkit.Corner.BOTTOM_LEFT -> margin to (height - boxHeight - margin)
            InterfaceToolkit.Corner.BOTTOM_RIGHT -> (width - boxWidth - margin) to (height - boxHeight - margin)
        }
        canvas.drawRect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight, fpsBg)
        canvas.drawText(label, boxLeft + pad, boxTop + pad + fpsText.textSize, fpsText)
    }

    private companion object {
        /** Pink RGB for touch markers; alpha is applied per-draw. */
        const val TOUCH_RGB = 0xFF4081
    }
}
