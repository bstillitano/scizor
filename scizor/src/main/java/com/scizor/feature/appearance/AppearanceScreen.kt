@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.scizor.feature.appearance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionHeader("Appearance")
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
        Text(
            "Forces the app's light/dark mode (Android 12+).",
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
