package com.scizor.feature.custom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scizor.ui.CopyMenuHost
import com.scizor.ui.EmptyState
import com.scizor.ui.KeyValueContent
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.Icons
import com.scizor.ui.rememberSearchQuery
import com.scizor.ui.ScizorListItem
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors

@Composable
internal fun EnvironmentVariablesScreen(viewModel: EnvironmentVariablesViewModel = viewModel()) {
    val all = viewModel.variables()
    val query = rememberSearchQuery("Search variables")

    if (all.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Code,
            title = "No environment variables",
            description = "Assign Scizor.environmentVariables in your app to list them here.",
        )
        return
    }

    val variables = all.filter { (k, v) ->
        query.isBlank() || k.contains(query, true) || v.contains(query, true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionHeader("Custom Key/Values")
        SegmentedColumn(items = variables) { (key, value), shapes ->
            CopyMenuHost(
                options = listOf(
                    "Copy value" to value,
                    "Copy key & value" to "$key: $value",
                ),
            ) { onLongClick ->
                ScizorListItem(
                    shapes = shapes,
                    colors = scizorSegmentedColors(),
                    onClick = null,
                    onLongClick = onLongClick,
                    content = { KeyValueContent(key = key, value = value) },
                )
            }
        }
        Text(
            "Long-press a row to copy.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
        )
    }
}
