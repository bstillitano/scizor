package com.scizor.feature.location

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A named preset coordinate. */
internal data class PresetLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

/** The currently applied mock location. */
internal data class MockLocation(val latitude: Double, val longitude: Double, val label: String)

/**
 * Applies mock GPS locations via [LocationManager] test providers. Requires the
 * app to be selected as the "mock location app" in Developer options; otherwise
 * [start] fails gracefully and returns false.
 */
internal object LocationSpoofer {

    val presets: List<PresetLocation> = listOf(
        PresetLocation("San Francisco", 37.7749, -122.4194),
        PresetLocation("New York", 40.7128, -74.0060),
        PresetLocation("London", 51.5074, -0.1278),
        PresetLocation("Paris", 48.8566, 2.3522),
        PresetLocation("Berlin", 52.5200, 13.4050),
        PresetLocation("Sydney", -33.8688, 151.2093),
        PresetLocation("Tokyo", 35.6762, 139.6503),
        PresetLocation("Singapore", 1.3521, 103.8198),
        PresetLocation("Dubai", 25.2048, 55.2708),
        PresetLocation("São Paulo", -23.5505, -46.6333),
        PresetLocation("Cape Town", -33.9249, 18.4241),
        PresetLocation("Toronto", 43.6532, -79.3832),
    )

    private val _active = MutableStateFlow<MockLocation?>(null)
    val active: StateFlow<MockLocation?> = _active.asStateFlow()

    private val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

    fun start(context: Context, latitude: Double, longitude: Double, label: String): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        val ok = runCatching {
            providers.forEach { provider ->
                runCatching {
                    manager.addTestProvider(
                        provider, false, false, false, false, true, true, true, 1, 1,
                    )
                    manager.setTestProviderEnabled(provider, true)
                    manager.setTestProviderLocation(provider, mockLocation(provider, latitude, longitude))
                }
            }
        }.isSuccess
        if (ok) _active.value = MockLocation(latitude, longitude, label)
        return ok
    }

    fun stop(context: Context) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        providers.forEach { provider ->
            runCatching { manager.removeTestProvider(provider) }
        }
        _active.value = null
    }

    private fun mockLocation(provider: String, lat: Double, lng: Double): Location =
        Location(provider).apply {
            latitude = lat
            longitude = lng
            accuracy = 1f
            altitude = 0.0
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
}
