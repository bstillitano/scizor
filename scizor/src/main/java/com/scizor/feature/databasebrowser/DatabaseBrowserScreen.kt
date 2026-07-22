@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.scizor.feature.databasebrowser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scizor.ui.ScizorNavigator
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors

private const val PAGE_SIZE = 50

@Composable
internal fun DatabaseBrowserScreen(navigator: ScizorNavigator) {
    val context = LocalContext.current
    val databases = remember { DatabaseBrowser.databases(context) }

    if (databases.isEmpty()) {
        EmptyState("No SQLite databases found in this app.")
        return
    }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionHeader("Databases")
        SegmentedColumn(items = databases) { db, shapes ->
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                leadingContent = { Icon(Icons.Filled.Storage, null, tint = MaterialTheme.colorScheme.primary) },
                supportingContent = { Text(formatBytes(db.sizeBytes)) },
                trailingContent = { Chevron() },
                modifier = Modifier.clickable { navigator.push(db.name) { TablesScreen(db.name, navigator) } },
                content = { Text(db.name) },
            )
        }
    }
}

@Composable
private fun TablesScreen(dbName: String, navigator: ScizorNavigator) {
    val context = LocalContext.current
    val tables = remember(dbName) { DatabaseBrowser.tables(context, dbName) }
    var query by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search tables") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        SectionHeader("Query")
        SegmentedColumn(items = listOf("sql")) { _, shapes ->
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                leadingContent = { Icon(Icons.Filled.PlayArrow, null, tint = MaterialTheme.colorScheme.primary) },
                supportingContent = { Text("Run raw SQL against this database") },
                trailingContent = { Chevron() },
                modifier = Modifier.clickable { navigator.push("SQL") { SqlScreen(dbName) } },
                content = { Text("SQL editor") },
            )
        }

        val filtered = tables.filter { query.isBlank() || it.name.contains(query, true) }
        if (filtered.isEmpty()) {
            EmptyState("No tables match.")
            return@Column
        }
        SectionHeader("Tables & views")
        SegmentedColumn(items = filtered) { table, shapes ->
            val count = remember(dbName, table.name) { DatabaseBrowser.count(context, dbName, table.name) }
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                leadingContent = {
                    Icon(
                        if (table.isView) Icons.Filled.Visibility else Icons.Filled.TableChart,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                supportingContent = { Text(if (table.isView) "View · $count rows" else "$count rows") },
                trailingContent = { Chevron() },
                modifier = Modifier.clickable {
                    navigator.push(table.name) { TableDataScreen(dbName, table.name, navigator) }
                },
                content = { Text(table.name) },
            )
        }
    }
}

@Composable
private fun TableDataScreen(dbName: String, table: String, navigator: ScizorNavigator) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    var offset by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }

    val total = remember(dbName, table, refreshKey) { DatabaseBrowser.count(context, dbName, table) }
    val schema = remember(dbName, table) { DatabaseBrowser.schema(context, dbName, table) }
    val data = remember(dbName, table, offset, refreshKey) {
        DatabaseBrowser.rows(context, dbName, table, PAGE_SIZE, offset)
    }
    val pkColumn = schema.columns.firstOrNull { it.primaryKey }?.name
    var adding by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionHeader("Schema")
        SegmentedColumn(items = listOf("schema")) { _, shapes ->
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                supportingContent = { Text("${schema.columns.size} columns") },
                trailingContent = { Chevron() },
                modifier = Modifier.clickable { navigator.push("Schema") { SchemaScreen(schema) } },
                content = { Text("Columns & indexes") },
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Filter this page") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )

        val shown = if (query.isBlank()) data.rows else data.rows.filter { row ->
            row.any { it.contains(query, true) }
        }
        val end = (offset + data.rows.size).coerceAtMost(total)
        val label = if (total == 0) "No records" else "Records ${offset + 1}–$end of $total"
        SectionHeader(label)

        if (shown.isEmpty()) {
            EmptyState(if (total == 0) "This table is empty." else "No rows match on this page.")
        } else {
            SegmentedColumn(items = shown) { row, shapes ->
                val first = data.columns.firstOrNull()?.let { "$it: ${row.firstOrNull().orEmpty()}" } ?: "—"
                val rest = data.columns.drop(1).take(2)
                    .mapIndexed { i, col -> "$col: ${row.getOrElse(i + 1) { "" }}" }
                    .joinToString("   ·   ")
                SegmentedListItem(
                    shapes = shapes,
                    colors = scizorSegmentedColors(),
                    supportingContent = {
                        if (rest.isNotBlank()) Text(rest, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    trailingContent = { Chevron() },
                    modifier = Modifier.clickable {
                        navigator.push("Record") {
                            RecordDetailScreen(dbName, table, data.columns, row, pkColumn) {
                                refreshKey++
                                navigator.pop()
                            }
                        }
                    },
                    content = { Text(first, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }

        if (total > PAGE_SIZE) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { offset = (offset - PAGE_SIZE).coerceAtLeast(0) },
                    enabled = offset > 0,
                    modifier = Modifier.weight(1f),
                ) { Text("Previous") }
                OutlinedButton(
                    onClick = { offset += PAGE_SIZE },
                    enabled = offset + PAGE_SIZE < total,
                    modifier = Modifier.weight(1f),
                ) { Text("Next") }
            }
        }

        OutlinedButton(
            onClick = { adding = true },
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, null)
            Text("Add record", modifier = Modifier.padding(start = 8.dp))
        }
    }

    if (adding) {
        EditRecordDialog(
            columns = schema.columns.map { it.name },
            row = emptyList(),
            title = "Add record",
            onDismiss = { adding = false },
            onSave = { values ->
                DatabaseBrowser.insertRow(context, dbName, table, values)
                adding = false
                refreshKey++
            },
        )
    }
}

@Composable
private fun SchemaScreen(schema: Schema) {
    val clipboard = LocalClipboardManager.current
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionHeader("Columns")
        SegmentedColumn(items = schema.columns) { col, shapes ->
            val attrs = buildList {
                add(col.type.ifBlank { "—" })
                if (col.primaryKey) add("PK")
                if (col.notNull) add("NOT NULL")
                col.default?.let { add("default $it") }
            }.joinToString("  ·  ")
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                supportingContent = { Text(attrs, style = MaterialTheme.typography.bodySmall) },
                trailingContent = {
                    if (col.primaryKey) AssistChip(onClick = {}, label = { Text("PK") })
                },
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { clipboard.setText(AnnotatedString("${col.name} ${col.type}")) },
                ),
                content = { Text(col.name) },
            )
        }
        if (schema.foreignKeys.isNotEmpty()) {
            SectionHeader("Foreign keys")
            SegmentedColumn(items = schema.foreignKeys) { fk, shapes ->
                SegmentedListItem(shapes = shapes, colors = scizorSegmentedColors(), content = { Text(fk) })
            }
        }
        if (schema.indexes.isNotEmpty()) {
            SectionHeader("Indexes")
            SegmentedColumn(items = schema.indexes) { idx, shapes ->
                SegmentedListItem(shapes = shapes, colors = scizorSegmentedColors(), content = { Text(idx) })
            }
        }
    }
}

@Composable
private fun SqlScreen(dbName: String) {
    val context = LocalContext.current
    var sql by remember { mutableStateOf("SELECT * FROM sqlite_master") }
    var result by remember { mutableStateOf<QueryResult?>(null) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        OutlinedTextField(
            value = sql,
            onValueChange = { sql = it },
            label = { Text("SQL") },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { result = DatabaseBrowser.execute(context, dbName, sql) },
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Icon(Icons.Filled.PlayArrow, null)
            Text("Run", modifier = Modifier.padding(start = 8.dp))
        }

        val r = result
        if (r != null) {
            when {
                r.error != null -> Text(
                    r.error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                )
                !r.readOnly -> Text(
                    "Statement executed.",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp),
                )
                r.data != null -> ResultTable(r.data)
            }
        }
    }
}

@Composable
private fun ResultTable(data: TableData) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text("${data.rows.size} rows", style = MaterialTheme.typography.labelMedium)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp)) {
            Column {
                Text(
                    data.columns.joinToString("  |  "),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.primary,
                )
                data.rows.take(200).forEach { row ->
                    Text(
                        row.joinToString("  |  "),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordDetailScreen(
    dbName: String,
    table: String,
    columns: List<String>,
    row: List<String>,
    pkColumn: String?,
    onChanged: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val pkValue = pkColumn?.let { pk -> columns.indexOf(pk).takeIf { it >= 0 }?.let { row.getOrNull(it) } }
    val editable = pkColumn != null && pkValue != null

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionHeader("Values")
        val pairs = columns.mapIndexed { i, col -> col to row.getOrElse(i) { "" } }
        SegmentedColumn(items = pairs) { (col, value), shapes ->
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                supportingContent = { Text(value, style = MaterialTheme.typography.bodyMedium) },
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { clipboard.setText(AnnotatedString("$col: $value")) },
                ),
                content = { Text(col, color = MaterialTheme.colorScheme.primary) },
            )
        }
        SectionHeader("Actions")
        SegmentedColumn(items = listOf("edit", "delete")) { action, shapes ->
            val isDelete = action == "delete"
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                supportingContent = if (editable) null else {
                    { Text("Unavailable — table has no primary key") }
                },
                modifier = Modifier.clickable(enabled = editable) {
                    if (isDelete) confirmDelete = true else editing = true
                },
                content = {
                    Text(
                        if (isDelete) "Delete record" else "Edit record",
                        color = if (isDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                },
            )
        }
    }

    if (editing && editable) {
        EditRecordDialog(
            columns = columns,
            row = row,
            onDismiss = { editing = false },
            onSave = { values ->
                DatabaseBrowser.updateRow(context, dbName, table, pkColumn!!, pkValue!!, values)
                editing = false
                onChanged()
            },
        )
    }

    if (confirmDelete && editable) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete record?") },
            text = { Text("This permanently removes the row where $pkColumn = $pkValue.") },
            confirmButton = {
                TextButton(onClick = {
                    DatabaseBrowser.deleteRow(context, dbName, table, pkColumn!!, pkValue!!)
                    confirmDelete = false
                    onChanged()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun EditRecordDialog(
    columns: List<String>,
    row: List<String>,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit,
    title: String = "Edit record",
) {
    val edited = remember {
        columns.mapIndexed { i, col -> col to mutableStateOf(row.getOrElse(i) { "" }) }.toMap()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                columns.forEach { col ->
                    val state = edited.getValue(col)
                    OutlinedTextField(
                        value = state.value,
                        onValueChange = { state.value = it },
                        label = { Text(col) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(edited.mapValues { it.value.value }) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Chevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${bytes / (1024 * 1024)} MB"
}
