@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.scizor.feature.interfacetools

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors

private data class ToolToggle(
    val label: String,
    val checked: Boolean,
    val onChange: (Boolean) -> Unit,
)

@Composable
internal fun UiToolsScreen() {
    val grid by InterfaceToolkit.grid.collectAsStateWithLifecycle()
    val bounds by InterfaceToolkit.viewBounds.collectAsStateWithLifecycle()
    val touches by InterfaceToolkit.touches.collectAsStateWithLifecycle()
    val fps by InterfaceToolkit.fps.collectAsStateWithLifecycle()
    val slow by InterfaceToolkit.slowAnimations.collectAsStateWithLifecycle()

    val tools = listOf(
        ToolToggle("Grid overlay", grid, InterfaceToolkit::setGrid),
        ToolToggle("Show view bounds", bounds, InterfaceToolkit::setViewBounds),
        ToolToggle("Touch visualiser", touches, InterfaceToolkit::setTouches),
        ToolToggle("FPS counter", fps, InterfaceToolkit::setFps),
        ToolToggle("Slow animations", slow, InterfaceToolkit::setSlowAnimations),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionHeader("Interface Tools")
        SegmentedColumn(items = tools) { tool, shapes ->
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                trailingContent = {
                    Switch(checked = tool.checked, onCheckedChange = tool.onChange)
                },
                content = { Text(tool.label) },
            )
        }
        Text(
            "Overlays draw on top of your app's own screens, not the Scizor menu.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
        )
    }
}
