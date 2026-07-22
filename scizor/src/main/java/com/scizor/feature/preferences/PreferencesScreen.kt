@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.scizor.feature.preferences

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scizor.ui.ScizorNavigator
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentInset
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors

@Composable
internal fun PreferencesScreen(
    navigator: ScizorNavigator,
    viewModel: PreferencesViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<PrefEntry?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    if (state.files.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No SharedPreferences files found.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val entries = state.entries.filter {
        query.isBlank() || it.key.contains(query, true) || it.value.contains(query, true)
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
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search keys and values") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = SegmentInset),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            if (entries.isEmpty()) {
                item {
                    Text(
                        if (state.entries.isEmpty()) "No entries in this file." else "No matching items.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    )
                }
            }
            itemsIndexed(entries, key = { _, it -> it.key }) { index, entry ->
                PrefRow(
                    entry = entry,
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = entries.size),
                    onToggleBool = { viewModel.setBoolean(entry.key, it) },
                    onClickRow = {
                        when (entry.type) {
                            "StringSet" -> navigator.push(entry.key) {
                                StringSetScreen(viewModel.stringSet(entry.key))
                            }
                            "Boolean", "null", "Unknown" -> {}
                            else -> editing = entry
                        }
                    },
                    onDelete = { viewModel.remove(entry.key) },
                )
            }
            if (query.isBlank()) {
                item {
                    TextButton(
                        onClick = { confirmReset = true },
                        modifier = Modifier.padding(vertical = 8.dp),
                    ) {
                        Text("Reset “${state.selectedFile}”", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    editing?.let { entry ->
        EditValueDialog(
            entry = entry,
            onDismiss = { editing = null },
            onSave = { text ->
                val ok = applyEdit(entry, text, viewModel)
                if (ok) editing = null
                ok
            },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset preferences?") },
            text = { Text("This permanently deletes every value in “${state.selectedFile}”. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetAll(); confirmReset = false }) {
                    Text("Reset all", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

private fun applyEdit(entry: PrefEntry, text: String, viewModel: PreferencesViewModel): Boolean {
    return when (entry.type) {
        "Int" -> text.toIntOrNull()?.let { viewModel.setInt(entry.key, it); true } ?: false
        "Long" -> text.toLongOrNull()?.let { viewModel.setLong(entry.key, it); true } ?: false
        "Float" -> text.toFloatOrNull()?.let { viewModel.setFloat(entry.key, it); true } ?: false
        else -> { viewModel.setString(entry.key, text); true }
    }
}

@Composable
private fun PrefRow(
    entry: PrefEntry,
    shapes: ListItemShapes,
    onToggleBool: (Boolean) -> Unit,
    onClickRow: () -> Unit,
    onDelete: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var menu by remember { mutableStateOf(false) }
    val isBool = entry.type.equals("Boolean", true)
    Box {
        SegmentedListItem(
            shapes = shapes,
            colors = scizorSegmentedColors(),
            supportingContent = { Text("${entry.value}  ·  ${entry.type}") },
            trailingContent = {
                if (isBool) {
                    Switch(checked = entry.value.toBoolean(), onCheckedChange = onToggleBool)
                }
            },
            modifier = Modifier.combinedClickable(
                onClick = { if (!isBool) onClickRow() },
                onLongClick = { menu = true },
            ),
            content = { Text(entry.key) },
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Copy value") }, onClick = {
                clipboard.setText(AnnotatedString(entry.value)); menu = false
            })
            DropdownMenuItem(text = { Text("Copy key & value") }, onClick = {
                clipboard.setText(AnnotatedString("${entry.key}: ${entry.value}")); menu = false
            })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { onDelete(); menu = false })
        }
    }
}

@Composable
private fun EditValueDialog(
    entry: PrefEntry,
    onDismiss: () -> Unit,
    onSave: (String) -> Boolean,
) {
    val numeric = entry.type in setOf("Int", "Long", "Float")
    var text by remember { mutableStateOf(entry.value) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.key) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; error = false },
                    isError = error,
                    singleLine = !numeric && entry.value.length < 40,
                    label = { Text("Value (${entry.type})") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
                    ),
                )
                if (error) {
                    Text("Enter a valid ${entry.type}.", color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (!onSave(text)) error = true }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun StringSetScreen(items: List<String>) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Empty set.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionHeader("String Set (${items.size})")
        SegmentedColumn(items = items) { value, shapes ->
            SegmentedListItem(shapes = shapes, colors = scizorSegmentedColors(), content = { Text(value) })
        }
    }
}
