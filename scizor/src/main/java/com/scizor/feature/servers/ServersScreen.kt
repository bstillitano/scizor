@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.scizor.feature.servers

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scizor.ui.CopyMenuHost
import com.scizor.ui.rememberSearchQuery
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors

@Composable
internal fun ServersScreen(viewModel: ServersViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query = rememberSearchQuery("Search name or variable key/values")

    if (state.environments.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No server environments configured.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        }
        return
    }

    val configs = state.environments
        .filter { env ->
            query.isBlank() ||
                env.name.contains(query, ignoreCase = true) ||
                env.baseUrl.contains(query, ignoreCase = true) ||
                env.variables.any { (k, v) -> k.contains(query, true) || v.contains(query, true) }
        }
        .sortedBy { it.name.lowercase() }

    val selected = state.environments.firstOrNull { it.name == state.selectedName }
    val selectedVars = buildList {
        selected?.let {
            add("baseUrl" to it.baseUrl)
            addAll(it.variables.entries.sortedBy { e -> e.key }.map { e -> e.key to e.value })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionHeader("Configuration")
        if (configs.isEmpty()) {
            EmptyRow("No matching configurations")
        } else {
            SegmentedColumn(items = configs) { environment, shapes ->
                val isSelected = environment.name == state.selectedName
                SegmentedListItem(
                    onClick = { viewModel.select(environment) },
                    shapes = shapes,
                    colors = scizorSegmentedColors(),
                    supportingContent = { Text(environment.baseUrl) },
                    trailingContent = {
                        if (isSelected) {
                            Icon(Icons.Filled.Check, "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    content = { Text(environment.name) },
                )
            }
        }

        SectionHeader("Variables")
        if (selectedVars.isEmpty()) {
            EmptyRow("No variables")
        } else {
            SegmentedColumn(items = selectedVars) { (key, value), shapes ->
                CopyMenuHost(
                    options = listOf("Copy value" to value, "Copy key & value" to "$key: $value"),
                ) { onLongClick ->
                    SegmentedListItem(
                        shapes = shapes,
                        colors = scizorSegmentedColors(),
                        supportingContent = { Text(value) },
                        modifier = Modifier.combinedClickable(onClick = {}, onLongClick = onLongClick),
                        content = { Text(key) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
    )
}
