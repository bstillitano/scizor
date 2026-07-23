@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.scizor.feature.interfacetools

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors

private val FpsGood = Color(0xFF2E7D32)
private val FpsOk = Color(0xFFE0932F)
private val FpsPoor = Color(0xFFD23B3B)

/** Overlays live in a system window, so enabling one needs "Display over other apps". */
private fun overlayToggle(context: Context, setter: (Boolean) -> Unit): (Boolean) -> Unit = { on ->
    setter(on)
    if (on && !OverlayController.canDrawOverlays(context)) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

@Composable
private fun MasterToggle(label: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    SegmentedColumn(items = listOf("master")) { _, shapes ->
        SegmentedListItem(
            shapes = shapes,
            colors = scizorSegmentedColors(),
            supportingContent = { Text(subtitle) },
            trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
            content = { Text(label, style = MaterialTheme.typography.titleMedium) },
        )
    }
}

@Composable
private fun <T> Chooser(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    SegmentedColumn(items = options) { option, shapes ->
        SegmentedListItem(
            shapes = shapes,
            colors = scizorSegmentedColors(),
            trailingContent = {
                if (option == selected) {
                    Icon(Icons.Filled.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                }
            },
            modifier = Modifier.clickable { onSelect(option) },
            content = { Text(label(option)) },
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, shapes: androidx.compose.material3.ListItemShapes) {
    SegmentedListItem(
        shapes = shapes,
        colors = scizorSegmentedColors(),
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        content = { Text(label) },
    )
}

@Composable
private fun OverlayNote() {
    Text(
        "Overlays draw over everything on screen, including the Scizor menu and system bars.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
    )
}

@Composable
internal fun GridOverlayScreen() {
    val context = LocalContext.current
    val enabled by InterfaceToolkit.grid.collectAsStateWithLifecycle()
    val size by InterfaceToolkit.gridSizeDp.collectAsStateWithLifecycle()
    val opacity by InterfaceToolkit.gridOpacity.collectAsStateWithLifecycle()
    val color by InterfaceToolkit.gridColor.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionHeader("Grid overlay")
        MasterToggle("Enable grid", "A spacing grid over your UI", enabled, overlayToggle(context, InterfaceToolkit::setGrid))
        if (enabled) {
            SectionHeader("Grid options")
            Column(modifier = Modifier.padding(16.dp)) {
                SliderRow("Grid size", "$size dp", size.toFloat(), 1f..100f) { InterfaceToolkit.setGridSizeDp(it.toInt()) }
                SliderRow("Opacity", "$opacity%", opacity.toFloat(), 1f..100f) { InterfaceToolkit.setGridOpacity(it.toInt()) }
            }
            SectionHeader("Grid color")
            Chooser(
                options = InterfaceToolkit.GridColor.entries,
                selected = color,
                label = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                onSelect = InterfaceToolkit::setGridColor,
            )
        }
        OverlayNote()
    }
}

@Composable
internal fun FpsCounterScreen() {
    val context = LocalContext.current
    val enabled by InterfaceToolkit.fps.collectAsStateWithLifecycle()
    val corner by InterfaceToolkit.fpsCorner.collectAsStateWithLifecycle()
    val current by InterfaceToolkit.currentFps.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionHeader("FPS counter")
        MasterToggle(
            "Enable FPS counter",
            "A real-time frame-rate indicator",
            enabled,
            overlayToggle(context, InterfaceToolkit::setFps),
        )
        if (enabled) {
            SectionHeader("Position")
            Chooser(
                options = InterfaceToolkit.Corner.entries,
                selected = corner,
                label = { it.displayName() },
                onSelect = InterfaceToolkit::setFpsCorner,
            )
            SectionHeader("Status")
            SegmentedColumn(items = listOf("fps")) { _, shapes ->
                SegmentedListItem(
                    shapes = shapes,
                    colors = scizorSegmentedColors(),
                    trailingContent = {
                        Text(
                            "$current",
                            style = MaterialTheme.typography.titleMedium,
                            color = fpsColor(current),
                        )
                    },
                    content = { Text("Current FPS") },
                )
            }
            Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)) {
                Legend(FpsGood, "55+ FPS · Excellent")
                Legend(FpsOk, "30–54 FPS · Acceptable")
                Legend(FpsPoor, "Below 30 FPS · Poor")
            }
        }
        OverlayNote()
    }
}

@Composable
internal fun TouchVisualiserScreen() {
    val context = LocalContext.current
    val enabled by InterfaceToolkit.touches.collectAsStateWithLifecycle()
    val duration by InterfaceToolkit.showTouchDuration.collectAsStateWithLifecycle()
    val showRadius by InterfaceToolkit.showTouchRadius.collectAsStateWithLifecycle()
    val logging by InterfaceToolkit.touchLogging.collectAsStateWithLifecycle()
    val log by InterfaceToolkit.touchLog.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionHeader("Touch visualiser")
        MasterToggle(
            "Show screen touches",
            "Marks every touch and follows drags",
            enabled,
            overlayToggle(context, InterfaceToolkit::setTouches),
        )
        if (enabled) {
            SectionHeader("Options")
            val rows = listOf("duration", "radius", "log")
            SegmentedColumn(items = rows) { row, shapes ->
                when (row) {
                    "duration" -> ToggleRow("Show touch duration", duration, InterfaceToolkit::setShowTouchDuration, shapes)
                    "radius" -> ToggleRow("Show touch radius", showRadius, InterfaceToolkit::setShowTouchRadius, shapes)
                    else -> ToggleRow("Log screen touches", logging, InterfaceToolkit::setTouchLogging, shapes)
                }
            }
            if (log.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { InterfaceToolkit.clearTouchLog() }) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                }
                SectionHeader("Touch log")
                SegmentedColumn(items = log) { entry, shapes ->
                    SegmentedListItem(shapes = shapes, colors = scizorSegmentedColors(), content = { Text(entry) })
                }
            }
        }
        OverlayNote()
    }
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Icon(Icons.Filled.Circle, contentDescription = null, tint = color, modifier = Modifier.size(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: String,
    current: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = current, onValueChange = onChange, valueRange = range)
    }
}

private fun fpsColor(fps: Int): Color = when {
    fps >= 55 -> FpsGood
    fps >= 30 -> FpsOk
    else -> FpsPoor
}

private fun InterfaceToolkit.Corner.displayName(): String = when (this) {
    InterfaceToolkit.Corner.TOP_LEFT -> "Top Left"
    InterfaceToolkit.Corner.TOP_RIGHT -> "Top Right"
    InterfaceToolkit.Corner.BOTTOM_LEFT -> "Bottom Left"
    InterfaceToolkit.Corner.BOTTOM_RIGHT -> "Bottom Right"
}
