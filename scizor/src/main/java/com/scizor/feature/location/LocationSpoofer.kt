package com.scizor.feature.location

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import com.scizor.Scizor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** A named preset coordinate. */
internal data class PresetLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

/** A named movement path made of ordered waypoints. */
internal data class Route(
    val name: String,
    val waypoints: List<PresetLocation>,
)

/** The currently applied mock location. [moving] is true while replaying a route. */
internal data class MockLocation(
    val latitude: Double,
    val longitude: Double,
    val label: String,
    val moving: Boolean = false,
)

/**
 * Applies mock GPS locations via [LocationManager] test providers. Requires the
 * app to be selected as the "mock location app" in Developer options; otherwise
 * [start] fails gracefully and returns false.
 *
 * A background pusher re-emits the current fix every second so apps that expect
 * a stream of updates keep receiving one, and [startRoute] interpolates between
 * waypoints to simulate continuous movement.
 */
internal object LocationSpoofer {

    private const val LAST_LAT = "location_last_lat"
    private const val LAST_LNG = "location_last_lng"
    private const val LAST_LABEL = "location_last_label"
    private const val PUSH_INTERVAL_MS = 1_000L
    private const val ROUTE_STEP_MS = 1_000L
    private const val ROUTE_SUBDIVISIONS = 20

    val presets: List<PresetLocation> = listOf(
        PresetLocation("San Francisco", 37.7749, -122.4194),
        PresetLocation("New York", 40.7128, -74.0060),
        PresetLocation("Los Angeles", 34.0522, -118.2437),
        PresetLocation("Chicago", 41.8781, -87.6298),
        PresetLocation("Toronto", 43.6532, -79.3832),
        PresetLocation("Mexico City", 19.4326, -99.1332),
        PresetLocation("São Paulo", -23.5505, -46.6333),
        PresetLocation("London", 51.5074, -0.1278),
        PresetLocation("Paris", 48.8566, 2.3522),
        PresetLocation("Berlin", 52.5200, 13.4050),
        PresetLocation("Madrid", 40.4168, -3.7038),
        PresetLocation("Rome", 41.9028, 12.4964),
        PresetLocation("Amsterdam", 52.3676, 4.9041),
        PresetLocation("Stockholm", 59.3293, 18.0686),
        PresetLocation("Moscow", 55.7558, 37.6173),
        PresetLocation("Dubai", 25.2048, 55.2708),
        PresetLocation("Mumbai", 19.0760, 72.8777),
        PresetLocation("Singapore", 1.3521, 103.8198),
        PresetLocation("Hong Kong", 22.3193, 114.1694),
        PresetLocation("Tokyo", 35.6762, 139.6503),
        PresetLocation("Seoul", 37.5665, 126.9780),
        PresetLocation("Sydney", -33.8688, 151.2093),
        PresetLocation("Cape Town", -33.9249, 18.4241),
    )

    val routes: List<Route> = listOf(
        Route(
            "SF → Palo Alto (US-101)",
            listOf(
                PresetLocation("San Francisco", 37.7749, -122.4194),
                PresetLocation("SFO", 37.6213, -122.3790),
                PresetLocation("San Mateo", 37.5630, -122.3255),
                PresetLocation("Palo Alto", 37.4419, -122.1430),
            ),
        ),
        Route(
            "Manhattan loop",
            listOf(
                PresetLocation("Times Square", 40.7580, -73.9855),
                PresetLocation("Central Park", 40.7812, -73.9665),
                PresetLocation("Upper East Side", 40.7736, -73.9566),
                PresetLocation("Midtown", 40.7549, -73.9840),
            ),
        ),
        Route(
            "Central London walk",
            listOf(
                PresetLocation("Westminster", 51.4995, -0.1248),
                PresetLocation("Trafalgar Square", 51.5080, -0.1281),
                PresetLocation("Covent Garden", 51.5117, -0.1240),
                PresetLocation("St Paul's", 51.5138, -0.0984),
            ),
        ),
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pushJob: Job? = null

    private val _active = MutableStateFlow<MockLocation?>(null)
    val active: StateFlow<MockLocation?> = _active.asStateFlow()

    private val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

    /** The last static fix that was applied, read from storage (not currently active). */
    fun lastLocation(): MockLocation? {
        val store = Scizor.storeOrNull() ?: return null
        val lat = store.string(LAST_LAT)?.toDoubleOrNull() ?: return null
        val lng = store.string(LAST_LNG)?.toDoubleOrNull() ?: return null
        return MockLocation(lat, lng, store.string(LAST_LABEL) ?: "Last location", moving = false)
    }

    fun start(context: Context, latitude: Double, longitude: Double, label: String): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        if (!register(manager)) return false
        persist(latitude, longitude, label)
        _active.value = MockLocation(latitude, longitude, label, moving = false)
        startPusher(manager)
        return true
    }

    fun startRoute(context: Context, route: Route): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        if (!register(manager)) return false
        val path = interpolate(route.waypoints)
        if (path.isEmpty()) return false
        pushJob?.cancel()
        pushJob = scope.launch {
            var index = 0
            while (isActive) {
                val point = path[index % path.size]
                push(manager, point.first, point.second)
                _active.value = MockLocation(point.first, point.second, route.name, moving = true)
                index++
                delay(ROUTE_STEP_MS)
            }
        }
        return true
    }

    fun stop(context: Context) {
        pushJob?.cancel()
        pushJob = null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        manager?.let { m -> providers.forEach { runCatching { m.removeTestProvider(it) } } }
        _active.value = null
    }

    private fun register(manager: LocationManager): Boolean = runCatching {
        providers.forEach { provider ->
            runCatching {
                manager.addTestProvider(provider, false, false, false, false, true, true, true, 1, 1)
                manager.setTestProviderEnabled(provider, true)
            }
        }
    }.isSuccess

    private fun startPusher(manager: LocationManager) {
        pushJob?.cancel()
        val current = _active.value ?: return
        pushJob = scope.launch {
            while (isActive) {
                push(manager, current.latitude, current.longitude)
                delay(PUSH_INTERVAL_MS)
            }
        }
    }

    private fun push(manager: LocationManager, lat: Double, lng: Double) {
        providers.forEach { provider ->
            runCatching { manager.setTestProviderLocation(provider, mockLocation(provider, lat, lng)) }
        }
    }

    /** Splits each waypoint segment into [ROUTE_SUBDIVISIONS] linear steps. */
    private fun interpolate(waypoints: List<PresetLocation>): List<Pair<Double, Double>> {
        if (waypoints.size < 2) return waypoints.map { it.latitude to it.longitude }
        return buildList {
            waypoints.zipWithNext { a, b ->
                for (step in 0 until ROUTE_SUBDIVISIONS) {
                    val t = step.toDouble() / ROUTE_SUBDIVISIONS
                    add(
                        (a.latitude + (b.latitude - a.latitude) * t) to
                            (a.longitude + (b.longitude - a.longitude) * t),
                    )
                }
            }
            add(waypoints.last().latitude to waypoints.last().longitude)
        }
    }

    private fun persist(lat: Double, lng: Double, label: String) {
        Scizor.storeOrNull()?.apply {
            putString(LAST_LAT, lat.toString())
            putString(LAST_LNG, lng.toString())
            putString(LAST_LABEL, label)
        }
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
