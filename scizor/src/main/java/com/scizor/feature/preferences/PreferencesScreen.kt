package com.scizor.feature.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
internal fun PreferencesScreen(viewModel: PreferencesViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<PrefEntry?>(null) }

    if (state.files.isEmpty()) {
        EmptyMessage("No SharedPreferences files found.")
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.files.forEach { file ->
                FilterChip(
                    selected = file == state.selectedFile,
                    onClick = { viewModel.selectFile(file) },
                    label = { Text(file) },
                )
            }
        }
        HorizontalDivider()

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.entries, key = { it.key }) { entry ->
                PrefRow(
                    entry = entry,
                    onToggle = { viewModel.setBoolean(entry.key, it) },
                    onEdit = { editing = entry },
                    onDelete = { viewModel.remove(entry.key) },
                )
                HorizontalDivider()
            }
        }
    }

    editing?.let { entry ->
        EditStringDialog(
            entry = entry,
            onDismiss = { editing = null },
            onSave = { newValue ->
                viewModel.setString(entry.key, newValue)
                editing = null
            },
        )
    }
}

@Composable
private fun PrefRow(
    entry: PrefEntry,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val isBoolean = entry.type.equals("Boolean", ignoreCase = true)
    ListItem(
        headlineContent = { Text(entry.key) },
        supportingContent = { Text("${entry.value}  ·  ${entry.type}") },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isBoolean) {
                    Switch(
                        checked = entry.value.toBoolean(),
                        onCheckedChange = onToggle,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
        },
        modifier = if (isBoolean) Modifier else Modifier.clickableRow(onEdit),
    )
}

@Composable
private fun EditStringDialog(
    entry: PrefEntry,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(entry.value) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Text(entry.key, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(onClick = { onSave(text) }) { Text("Save") }
            }
        }
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
