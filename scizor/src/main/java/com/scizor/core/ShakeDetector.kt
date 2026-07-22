package com.scizor.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Detects a shake gesture from the accelerometer and invokes [onShake].
 *
 * A shake is registered when the net g-force exceeds [SHAKE_THRESHOLD_G] and at
 * least [DEBOUNCE_MS] have elapsed since the previous detection, preventing a
 * single shake from firing repeatedly.
 */
internal class ShakeDetector(
    private val onShake: () -> Unit,
) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var lastShakeAt: Long = 0L

    fun start(context: Context) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        sensorManager = manager
        manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        if (isShake(event.values[0], event.values[1], event.values[2])) {
            val now = event.timestamp / 1_000_000L
            if (now - lastShakeAt < DEBOUNCE_MS) return
            lastShakeAt = now
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val SHAKE_THRESHOLD_G = 2.7f
        private const val DEBOUNCE_MS = 500L

        /** Returns true when the acceleration magnitude exceeds the shake threshold. */
        fun isShake(x: Float, y: Float, z: Float): Boolean {
            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH
            val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)
            return gForce > SHAKE_THRESHOLD_G
        }
    }
}
