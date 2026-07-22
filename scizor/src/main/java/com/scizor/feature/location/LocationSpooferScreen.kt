@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.scizor.feature.location

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors

@Composable
internal fun LocationSpooferScreen() {
    val context = LocalContext.current
    val active by LocationSpoofer.active.collectAsStateWithLifecycle()
    var lat by remember { mutableStateOf("") }
    var lng by remember { mutableStateOf("") }

    fun apply(latitude: Double, longitude: Double, label: String) {
        val ok = LocationSpoofer.start(context, latitude, longitude, label)
        Toast.makeText(
            context,
            if (ok) "Mock location set" else "Failed — set Scizor as the mock location app",
            Toast.LENGTH_SHORT,
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        active?.let { mock ->
            SectionHeader("Active")
            SegmentedColumn(items = listOf(mock)) { m, shapes ->
                SegmentedListItem(
                    shapes = shapes,
                    colors = scizorSegmentedColors(),
                    supportingContent = { Text("${m.latitude}, ${m.longitude}") },
                    content = { Text(m.label) },
                )
            }
            Button(
                onClick = { LocationSpoofer.stop(context) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("Stop mocking")
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
        ) {
            Text("Apply custom location")
        }

        SectionHeader("Presets")
        SegmentedColumn(items = LocationSpoofer.presets) { preset, shapes ->
            SegmentedListItem(
                onClick = { apply(preset.latitude, preset.longitude, preset.name) },
                shapes = shapes,
                colors = scizorSegmentedColors(),
                supportingContent = { Text("${preset.latitude}, ${preset.longitude}") },
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
