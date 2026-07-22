@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.scizor.feature.interfacetools

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scizor.ui.ScizorNavigator
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors

@Composable
internal fun UiToolsScreen(navigator: ScizorNavigator) {
    val context = LocalContext.current
    val grid by InterfaceToolkit.grid.collectAsStateWithLifecycle()
    val frames by InterfaceToolkit.frames.collectAsStateWithLifecycle()
    val sizes by InterfaceToolkit.sizes.collectAsStateWithLifecycle()
    val touches by InterfaceToolkit.touches.collectAsStateWithLifecycle()
    val fps by InterfaceToolkit.fps.collectAsStateWithLifecycle()
    val slow by InterfaceToolkit.slowAnimations.collectAsStateWithLifecycle()

    // Overlays live in a system window, so enabling one needs "Display over other apps".
    fun toggleOverlay(setter: (Boolean) -> Unit): (Boolean) -> Unit = { on ->
        setter(on)
        if (on && !OverlayController.canDrawOverlays(context)) {
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:${context.packageName}".toUri(),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (!OverlayController.canDrawOverlays(context)) {
            Text(
                "Overlays need the “Display over other apps” permission. Turning one on will ask for it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
            )
        }
        SectionHeader("Overlays")
        SegmentedColumn(items = OVERLAY_ROWS) { row, shapes ->
            val (checked, onChange, settings) = when (row) {
                Overlay.GRID -> Triple(grid, toggleOverlay(InterfaceToolkit::setGrid)) { navigator.push("Grid") { GridSettingsScreen() } }
                Overlay.FRAMES -> Triple(frames, toggleOverlay(InterfaceToolkit::setFrames), null)
                Overlay.SIZES -> Triple(sizes, toggleOverlay(InterfaceToolkit::setSizes), null)
                Overlay.TOUCHES -> Triple(touches, toggleOverlay(InterfaceToolkit::setTouches)) { navigator.push("Touches") { TouchSettingsScreen() } }
                Overlay.FPS -> Triple(fps, toggleOverlay(InterfaceToolkit::setFps)) { navigator.push("FPS") { FpsSettingsScreen() } }
            }
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                supportingContent = { Text(row.subtitle) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = checked, onCheckedChange = onChange)
                        if (settings != null) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                "Configure",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                modifier = if (settings != null) Modifier.clickable { settings() } else Modifier,
                content = { Text(row.label) },
            )
        }

        SectionHeader("Animation")
        SegmentedColumn(items = listOf("slow")) { _, shapes ->
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                supportingContent = { Text("Scale animator durations 10×") },
                trailingContent = { Switch(checked = slow, onCheckedChange = InterfaceToolkit::setSlowAnimations) },
                content = { Text("Slow animations") },
            )
        }

        Text(
            "Overlays draw over everything on screen, including the Scizor menu and system bars.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
        )
    }
}

private enum class Overlay(val label: String, val subtitle: String) {
    GRID("Grid overlay", "Spacing grid · tap to configure"),
    FRAMES("View frames", "Outline every view's bounds"),
    SIZES("View sizes", "Label each view's width × height"),
    TOUCHES("Touch visualiser", "Ripple on every touch · tap to configure"),
    FPS("FPS counter", "Frame rate meter · tap to configure"),
}

private val OVERLAY_ROWS = Overlay.entries

@Composable
private fun GridSettingsScreen() {
    val size by InterfaceToolkit.gridSizeDp.collectAsStateWithLifecycle()
    val opacity by InterfaceToolkit.gridOpacity.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        SliderRow("Grid size", "$size dp", size.toFloat(), 2f..32f) { InterfaceToolkit.setGridSizeDp(it.toInt()) }
        SliderRow("Opacity", "$opacity%", opacity.toFloat(), 2f..100f) { InterfaceToolkit.setGridOpacity(it.toInt()) }
    }
}

@Composable
private fun TouchSettingsScreen() {
    val radius by InterfaceToolkit.touchRadiusDp.collectAsStateWithLifecycle()
    val fade by InterfaceToolkit.touchFadeMs.collectAsStateWithLifecycle()
    val logging by InterfaceToolkit.touchLogging.collectAsStateWithLifecycle()
    val log by InterfaceToolkit.touchLog.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(modifier = Modifier.padding(16.dp)) {
            SliderRow("Touch radius", "$radius dp", radius.toFloat(), 4f..48f) {
                InterfaceToolkit.setTouchRadiusDp(it.toInt())
            }
            SliderRow("Fade duration", "$fade ms", fade.toFloat(), 200f..3000f) {
                InterfaceToolkit.setTouchFadeMs(it.toInt())
            }
        }
        SegmentedColumn(items = listOf("log")) { _, shapes ->
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                supportingContent = { Text("Record touch coordinates below") },
                trailingContent = { Switch(checked = logging, onCheckedChange = InterfaceToolkit::setTouchLogging) },
                content = { Text("Log touches") },
            )
        }
        if (log.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
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
}

@Composable
private fun FpsSettingsScreen() {
    val corner by InterfaceToolkit.fpsCorner.collectAsStateWithLifecycle()
    val warn by InterfaceToolkit.fpsWarn.collectAsStateWithLifecycle()
    val critical by InterfaceToolkit.fpsCritical.collectAsStateWithLifecycle()
    val averaged by InterfaceToolkit.fpsAveraged.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionHeader("Position")
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            InterfaceToolkit.Corner.entries.forEach { c ->
                FilterChip(
                    selected = corner == c,
                    onClick = { InterfaceToolkit.setFpsCorner(c) },
                    label = { Text(c.shortLabel()) },
                )
            }
        }
        Column(modifier = Modifier.padding(16.dp)) {
            SliderRow("Warning below", "$warn fps", warn.toFloat(), 1f..120f) { InterfaceToolkit.setFpsWarn(it.toInt()) }
            SliderRow("Critical below", "$critical fps", critical.toFloat(), 1f..120f) {
                InterfaceToolkit.setFpsCritical(it.toInt())
            }
        }
        SegmentedColumn(items = listOf("avg")) { _, shapes ->
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                supportingContent = { Text("Show a 60-frame rolling average") },
                trailingContent = { Switch(checked = averaged, onCheckedChange = InterfaceToolkit::setFpsAveraged) },
                content = { Text("Averaged") },
            )
        }
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

private fun InterfaceToolkit.Corner.shortLabel(): String = when (this) {
    InterfaceToolkit.Corner.TOP_LEFT -> "TL"
    InterfaceToolkit.Corner.TOP_RIGHT -> "TR"
    InterfaceToolkit.Corner.BOTTOM_LEFT -> "BL"
    InterfaceToolkit.Corner.BOTTOM_RIGHT -> "BR"
}
