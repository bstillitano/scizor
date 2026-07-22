@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.scizor.feature.appearance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
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

@Composable
internal fun AppearanceScreen() {
    val mode by AppearanceOverrides.mode.collectAsStateWithLifecycle()
    val fontScale by AppearanceOverrides.fontScale.collectAsStateWithLifecycle()
    val highContrast by AppearanceOverrides.highContrast.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionHeader("Theme")
        SegmentedColumn(items = AppearanceMode.entries.toList()) { option, shapes ->
            val selected = option == mode
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                leadingContent = {
                    RadioButton(selected = selected, onClick = { AppearanceOverrides.set(option) })
                },
                modifier = Modifier.selectable(
                    selected = selected,
                    onClick = { AppearanceOverrides.set(option) },
                ),
                content = { Text(option.label()) },
            )
        }

        SectionHeader("Font scale")
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Override", modifier = Modifier.weight(1f))
                Text("%.2f×".format(fontScale), color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = fontScale,
                onValueChange = { AppearanceOverrides.setFontScale(it) },
                valueRange = 0.85f..1.5f,
                steps = 12,
            )
            Text(
                "System scale is %.2f×. Requires Scizor.wrapAppearance(base) in your Activity.attachBaseContext."
                    .format(AppearanceOverrides.systemFontScale()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionHeader("Accessibility")
        SegmentedColumn(items = listOf("contrast")) { _, shapes ->
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                supportingContent = { Text("Flag hosts can read for high-contrast styling") },
                trailingContent = {
                    Switch(checked = highContrast, onCheckedChange = { AppearanceOverrides.setHighContrast(it) })
                },
                content = { Text("High contrast") },
            )
        }

        OutlinedButton(
            onClick = { AppearanceOverrides.reset() },
            modifier = Modifier.padding(16.dp),
        ) { Text("Reset to defaults") }

        Text(
            "Light/dark forcing uses Android 12+ per-app night mode.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
        )
    }
}

private fun AppearanceMode.label(): String = when (this) {
    AppearanceMode.SYSTEM -> "System default"
    AppearanceMode.LIGHT -> "Light"
    AppearanceMode.DARK -> "Dark"
}
