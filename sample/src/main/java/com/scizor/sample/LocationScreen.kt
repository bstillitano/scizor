package com.scizor.sample

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.scizor.Scizor
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.DateFormat
import java.util.Date

@Composable
fun LocationScreen() {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    var location by remember { mutableStateOf<Location?>(null) }

    // Android 12+ lets the user grant only approximate location, so either permission counts.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        hasPermission = grants.values.any { it }
    }

    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager }

    DisposableEffect(hasPermission) {
        if (hasPermission && locationManager != null) {
            val listener = LocationListener { location = it }
            bestLastKnown(context, locationManager)?.let { location = it }
            allowedProviders(context).forEach {
                runCatching {
                    if (locationManager.isProviderEnabled(it)) {
                        locationManager.requestLocationUpdates(it, 1000L, 0f, listener, Looper.getMainLooper())
                    }
                }
            }
            onDispose { runCatching { locationManager.removeUpdates(listener) } }
        } else {
            onDispose {}
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Section(
                "Scizor Location Spoofer",
                footer = "Configure spoofing in Scizor → System Tools → Location Spoofer. Set this app as the " +
                    "device's mock-location app first.",
                rows = listOf(
                    SampleRow.Info("The device location below reflects any active mock location."),
                ),
            )
        }

        item {
            val loc = location
            val rows = when {
                !hasPermission -> listOf(
                    SampleRow.Action("Request location permission") {
                        launcher.launch(LOCATION_PERMISSIONS)
                    },
                )
                loc == null -> listOf(SampleRow.Info("Fetching location…"))
                else -> listOf(
                    SampleRow.Label("Latitude", "%.6f".format(loc.latitude)),
                    SampleRow.Label("Longitude", "%.6f".format(loc.longitude)),
                    SampleRow.Label("Accuracy", "%.1f m".format(loc.accuracy)),
                    SampleRow.Label("Altitude", "%.1f m".format(loc.altitude)),
                    SampleRow.Label("Updated", DateFormat.getTimeInstance().format(Date(loc.time))),
                    SampleRow.Action("Refresh location") {
                        if (locationManager != null) bestLastKnown(context, locationManager)?.let { location = it }
                    },
                )
            }
            Section("Location Reports", rows = rows)
        }

        location?.let { loc ->
            item {
                Column {
                    Text(
                        "MAP",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
                    )
                    LocationMap(loc)
                }
            }
        }

        item {
            Section(
                "Actions",
                rows = listOf(
                    SampleRow.Action("Open Scizor menu") { Scizor.show() },
                ),
            )
        }
    }
}

@Composable
private fun LocationMap(location: Location) {
    val context = LocalContext.current
    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            clipToOutline = true
            controller.setZoom(15.0)
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    // Clip the map to a rounded box so osmdroid can't draw past the card's bounds.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.matchParentSize(),
            update = { view ->
                val point = GeoPoint(location.latitude, location.longitude)
                view.controller.setCenter(point)
                view.overlays.clear()
                view.overlays.add(
                    Marker(view).apply {
                        position = point
                        title = "Current location"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    },
                )
                view.invalidate()
            },
        )
    }
}

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

private fun isGranted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun hasLocationPermission(context: Context): Boolean =
    LOCATION_PERMISSIONS.any { isGranted(context, it) }

// GPS needs FINE; the network provider works with COARSE alone.
private fun allowedProviders(context: Context): List<String> = buildList {
    if (isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)) add(LocationManager.GPS_PROVIDER)
    if (hasLocationPermission(context)) add(LocationManager.NETWORK_PROVIDER)
}

private fun bestLastKnown(context: Context, manager: LocationManager): Location? =
    allowedProviders(context)
        .mapNotNull { runCatching { if (manager.isProviderEnabled(it)) manager.getLastKnownLocation(it) else null }.getOrNull() }
        .maxByOrNull { it.time }
