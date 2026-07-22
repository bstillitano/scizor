@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.scizor.feature.featureflags

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors

private enum class GlobalRow { ENABLE, RESET }

@Composable
internal fun FeatureFlagsScreen(viewModel: FeatureFlagsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionHeader("Global Settings")
        val globalRows = buildList {
            add(GlobalRow.ENABLE)
            if (state.overridesEnabled) add(GlobalRow.RESET)
        }
        SegmentedColumn(items = globalRows) { row, shapes ->
            when (row) {
                GlobalRow.ENABLE -> SegmentedListItem(
                    shapes = shapes,
                    colors = scizorSegmentedColors(),
                    trailingContent = {
                        Switch(
                            checked = state.overridesEnabled,
                            onCheckedChange = viewModel::setOverridesEnabled,
                        )
                    },
                    content = { Text("Enable overrides") },
                )
                GlobalRow.RESET -> SegmentedListItem(
                    onClick = viewModel::resetAll,
                    shapes = shapes,
                    colors = scizorSegmentedColors(),
                    content = { Text("Reset all to Remote", color = MaterialTheme.colorScheme.primary) },
                )
            }
        }

        if (!state.overridesEnabled) {
            Text(
                text = "Enable overrides to view and modify feature flags.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
            )
            return@Column
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search toggles") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )

        val filtered = state.flags.filter { it.title.contains(query, ignoreCase = true) }
        val pinned = filtered.filter { it.pinned }
        val rest = filtered.filterNot { it.pinned }

        if (pinned.isNotEmpty()) {
            SectionHeader("Pinned")
            FlagList(pinned, viewModel)
        }

        SectionHeader("Toggles")
        if (rest.isEmpty()) {
            Text(
                text = if (state.flags.isEmpty()) "No feature flags registered." else "No matching toggles.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
            )
        } else {
            FlagList(rest, viewModel)
        }
    }
}

@Composable
private fun FlagList(flags: List<FlagUi>, viewModel: FeatureFlagsViewModel) {
    SegmentedColumn(items = flags) { flag, shapes ->
        var pinMenu by remember { mutableStateOf(false) }
        Box {
            SegmentedListItem(
                onLongClick = { pinMenu = true },
                onClick = {},
                shapes = shapes,
                colors = scizorSegmentedColors(),
                supportingContent = { Text("Remote: ${flag.remoteValue}") },
                trailingContent = {
                    FlagStateDropdown(state = flag.state) { viewModel.setState(flag.key, it) }
                },
                content = { Text(flag.title) },
            )
            DropdownMenu(expanded = pinMenu, onDismissRequest = { pinMenu = false }) {
                DropdownMenuItem(
                    text = { Text(if (flag.pinned) "Unpin" else "Pin to top") },
                    onClick = {
                        viewModel.togglePin(flag.key)
                        pinMenu = false
                    },
                )
            }
        }
    }
}

@Composable
private fun FlagStateDropdown(state: FlagOverride, onSelect: (FlagOverride) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(state.label())
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FlagOverride.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label()) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun FlagOverride.label(): String = when (this) {
    FlagOverride.ON -> "True"
    FlagOverride.OFF -> "False"
    FlagOverride.REMOTE -> "Remote"
}
