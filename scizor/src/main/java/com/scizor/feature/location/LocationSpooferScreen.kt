@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.scizor.feature.location

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors

@Composable
private fun MiniMap(lat: Double, lng: Double) {
    val grid = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val dot = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        drawRect(color = surface)
        for (i in 1 until 6) {
            val x = size.width * i / 6f
            drawLine(grid, Offset(x, 0f), Offset(x, size.height))
        }
        for (i in 1 until 6) {
            val y = size.height * i / 6f
            drawLine(grid, Offset(0f, y), Offset(size.width, y))
        }
        // Equirectangular projection: lng [-180,180] → x, lat [90,-90] → y
        val px = ((lng + 180.0) / 360.0).toFloat() * size.width
        val py = ((90.0 - lat) / 180.0).toFloat() * size.height
        drawCircle(color = dot.copy(alpha = 0.25f), radius = 12.dp.toPx(), center = Offset(px, py))
        drawCircle(color = dot, radius = 5.dp.toPx(), center = Offset(px, py))
    }
}

@Composable
internal fun LocationSpooferScreen() {
    val context = LocalContext.current
    val active by LocationSpoofer.active.collectAsStateWithLifecycle()
    val last = remember { LocationSpoofer.lastLocation() }
    var lat by remember { mutableStateOf(last?.latitude?.toString().orEmpty()) }
    var lng by remember { mutableStateOf(last?.longitude?.toString().orEmpty()) }
    var query by remember { mutableStateOf("") }

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

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Master toggle
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Mock location", style = MaterialTheme.typography.titleMedium)
                Text(
                    active?.let {
                        if (it.moving) "Moving · ${it.label}" else "Active · ${it.label}"
                    } ?: "Off",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = active != null,
                onCheckedChange = { on ->
                    if (!on) {
                        LocationSpoofer.stop(context)
                    } else {
                        val la = lat.toDoubleOrNull()
                        val lo = lng.toDoubleOrNull()
                        if (la != null && lo != null) apply(la, lo, "Custom") else apply(37.7749, -122.4194, "San Francisco")
                    }
                },
            )
        }

        active?.let { mock ->
            MiniMap(mock.latitude, mock.longitude)
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

        SectionHeader("Cities")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search cities") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
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

        Text(
            "Requires 'Select mock location app' → Scizor Sample in Developer options.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
        )
    }
}
