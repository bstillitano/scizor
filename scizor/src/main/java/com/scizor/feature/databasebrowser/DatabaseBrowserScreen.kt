@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.scizor.feature.databasebrowser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scizor.ui.ScizorNavigator
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors

@Composable
internal fun DatabaseBrowserScreen(navigator: ScizorNavigator) {
    val context = LocalContext.current
    val databases = remember { DatabaseBrowser.databases(context) }

    if (databases.isEmpty()) {
        EmptyState("No SQLite databases found in this app.")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(databases, key = { it }) { db ->
            ListItem(
                leadingContent = { Icon(Icons.Filled.Storage, null, tint = MaterialTheme.colorScheme.primary) },
                headlineContent = { Text(db) },
                trailingContent = { Chevron() },
                modifier = Modifier.clickable {
                    navigator.push(db) { TablesScreen(db, navigator) }
                },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun TablesScreen(dbName: String, navigator: ScizorNavigator) {
    val context = LocalContext.current
    val tables = remember(dbName) { DatabaseBrowser.tables(context, dbName) }

    if (tables.isEmpty()) {
        EmptyState("This database contains no tables.")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(tables, key = { it }) { table ->
            ListItem(
                leadingContent = { Icon(Icons.Filled.TableChart, null, tint = MaterialTheme.colorScheme.primary) },
                headlineContent = { Text(table) },
                trailingContent = { Chevron() },
                modifier = Modifier.clickable {
                    navigator.push(table) { TableDataScreen(dbName, table, navigator) }
                },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun TableDataScreen(dbName: String, table: String, navigator: ScizorNavigator) {
    val context = LocalContext.current
    val data = remember(dbName, table) { DatabaseBrowser.rows(context, dbName, table) }

    if (data.rows.isEmpty()) {
        EmptyState("This table is empty.")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { SectionHeader("Records (${data.rows.size})") }
        itemsIndexed(data.rows) { index, row ->
            val first = data.columns.firstOrNull()?.let { "$it: ${row.firstOrNull().orEmpty()}" } ?: "Row ${index + 1}"
            val rest = data.columns.drop(1).take(2)
                .mapIndexed { i, col -> "$col: ${row.getOrElse(i + 1) { "" }}" }
                .joinToString("   ·   ")
            ListItem(
                headlineContent = { Text(first) },
                supportingContent = { if (rest.isNotBlank()) Text(rest, style = MaterialTheme.typography.bodySmall) },
                trailingContent = { Chevron() },
                modifier = Modifier.clickable {
                    navigator.push("Record") { RecordDetailScreen(data.columns, row) }
                },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun RecordDetailScreen(columns: List<String>, row: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionHeader("Values")
        val pairs = columns.mapIndexed { i, col -> col to row.getOrElse(i) { "" } }
        SegmentedColumn(items = pairs) { (col, value), shapes ->
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                supportingContent = { Text(value, style = MaterialTheme.typography.bodyMedium) },
                content = { Text(col, color = MaterialTheme.colorScheme.primary) },
            )
        }
    }
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
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp),
        )
    }
}
