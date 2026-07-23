@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.scizor.feature.location

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scizor.ui.rememberSearchQuery
import com.scizor.ui.rememberTopBarAction
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

/** A real OpenStreetMap of the mock location; tapping the map sets a new spoof point. */
@Composable
private fun SpooferMap(lat: Double, lng: Double, onTap: (Double, Double) -> Unit) {
    val context = LocalContext.current
    val latestOnTap by rememberUpdatedState(onTap)
    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(12.0)
        }
    }

    DisposableEffect(Unit) {
        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let { latestOnTap(it.latitude, it.longitude) }
                return true
            }
            override fun longPressHelper(p: GeoPoint?): Boolean = false
        }
        mapView.overlays.add(0, MapEventsOverlay(receiver))
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.matchParentSize(),
            update = { view ->
                val point = GeoPoint(lat, lng)
                view.controller.setCenter(point)
                view.overlays.removeAll { it is Marker }
                view.overlays.add(
                    Marker(view).apply {
                        position = point
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    },
                )
                view.invalidate()
            },
        )
        Text(
            "Tap the map to set a location",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                    RoundedCornerShape(6.dp),
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
internal fun LocationSpooferScreen() {
    val context = LocalContext.current
    val active by LocationSpoofer.active.collectAsStateWithLifecycle()
    val last = remember { LocationSpoofer.lastLocation() }
    var lat by remember { mutableStateOf(last?.latitude?.toString().orEmpty()) }
    var lng by remember { mutableStateOf(last?.longitude?.toString().orEmpty()) }

    rememberTopBarAction(Icons.Filled.Settings, "Open Developer options") {
        runCatching {
            context.startActivity(
                android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun apply(latitude: Double, longitude: Double, label: String) {
        val ok = LocationSpoofer.start(context, latitude, longitude, label)
        Toast.makeText(
            context,
            if (ok) "Mock location set" else "Failed — set Scizor as the mock location app",
            Toast.LENGTH_SHORT,
        ).show()
    }

    fun replay(route: Route) {
        val ok = LocationSpoofer.startRoute(context, route)
        Toast.makeText(
            context,
            if (ok) "Replaying ${route.name}" else "Failed — set Scizor as the mock location app",
            Toast.LENGTH_SHORT,
        ).show()
    }

    val mock = active

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionHeader("Mock location")
        SegmentedColumn(items = listOf("master")) { _, shapes ->
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                supportingContent = {
                    Text(
                        mock?.let {
                            if (it.moving) "Moving · ${it.label}" else "Active · ${it.label}"
                        } ?: "Off",
                    )
                },
                trailingContent = {
                    Switch(
                        checked = mock != null,
                        onCheckedChange = { on ->
                            if (!on) {
                                LocationSpoofer.stop(context)
                            } else {
                                val la = lat.toDoubleOrNull()
                                val lo = lng.toDoubleOrNull()
                                if (la != null && lo != null) apply(la, lo, "Custom")
                                else apply(37.7749, -122.4194, "San Francisco")
                            }
                        },
                    )
                },
                content = { Text("Mock location", style = MaterialTheme.typography.titleMedium) },
            )
        }

        if (mock != null) {
            SpooferMap(mock.latitude, mock.longitude) { la, lo ->
                lat = "%.6f".format(la)
                lng = "%.6f".format(lo)
                apply(la, lo, "Dropped pin")
            }
            SegmentedColumn(items = listOf(mock)) { m, shapes ->
                SegmentedListItem(
                    shapes = shapes,
                    colors = scizorSegmentedColors(),
                    leadingContent = {
                        Icon(
                            if (m.moving) Icons.Filled.DirectionsRun else Icons.Filled.LocationOn,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    supportingContent = { Text("%.5f, %.5f".format(m.latitude, m.longitude)) },
                    content = { Text(m.label) },
                )
            }

            SectionHeader("Custom")
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = lat,
                    onValueChange = { lat = it },
                    label = { Text("Latitude") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                OutlinedTextField(
                    value = lng,
                    onValueChange = { lng = it },
                    label = { Text("Longitude") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            Button(
                onClick = { apply(lat.toDouble(), lng.toDouble(), "Custom") },
                enabled = lat.toDoubleOrNull() != null && lng.toDoubleOrNull() != null,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) { Text("Apply custom location") }

            SectionHeader("Routes")
            SegmentedColumn(items = LocationSpoofer.routes) { route, shapes ->
                SegmentedListItem(
                    onClick = { replay(route) },
                    shapes = shapes,
                    colors = scizorSegmentedColors(),
                    leadingContent = { Icon(Icons.Filled.PlayArrow, null, tint = MaterialTheme.colorScheme.primary) },
                    supportingContent = { Text("${route.waypoints.size} waypoints") },
                    content = { Text(route.name) },
                )
            }

            val query = rememberSearchQuery("Search cities")
            SectionHeader("Cities")
            val cities = LocationSpoofer.presets.filter { query.isBlank() || it.name.contains(query, true) }
            SegmentedColumn(items = cities) { preset, shapes ->
                SegmentedListItem(
                    onClick = { apply(preset.latitude, preset.longitude, preset.name) },
                    shapes = shapes,
                    colors = scizorSegmentedColors(),
                    supportingContent = { Text("%.4f, %.4f".format(preset.latitude, preset.longitude)) },
                    content = { Text(preset.name) },
                )
            }
        }

        Text(
            "Mocking requires this app to be the device's mock-location app: Developer options → " +
                "“Select mock location app” (open it from the icon above). Enable Developer options " +
                "first (tap Build number 7×).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
        )
    }
}
