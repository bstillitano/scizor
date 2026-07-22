@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.scizor.feature.filebrowser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scizor.feature.network.TextReaderScreen
import com.scizor.ui.ScizorNavigator
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
internal fun FileBrowserScreen(navigator: ScizorNavigator) {
    val context = LocalContext.current
    val roots = FileBrowser.roots(context)

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(roots, key = { it.path }) { root ->
            ListItem(
                leadingContent = { Icon(Icons.Filled.Folder, null, tint = MaterialTheme.colorScheme.primary) },
                headlineContent = { Text(root.label) },
                supportingContent = { Text(root.path, style = MaterialTheme.typography.bodySmall) },
                trailingContent = { Chevron() },
                modifier = Modifier.clickable {
                    navigator.push(root.label) { DirectoryScreen(File(root.path), navigator) }
                },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun DirectoryScreen(dir: File, navigator: ScizorNavigator) {
    val nodes = FileBrowser.list(dir)
    if (nodes.isEmpty()) {
        EmptyState("Empty folder")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(nodes, key = { it.path }) { node ->
            ListItem(
                leadingContent = {
                    Icon(
                        imageVector = if (node.isDirectory) Icons.Filled.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                        contentDescription = null,
                        tint = if (node.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                headlineContent = { Text(node.name) },
                supportingContent = {
                    val sizePart = if (node.isDirectory) "folder" else FileBrowser.humanSize(node.size)
                    Text("$sizePart  ·  ${formatDate(node.lastModified)}", style = MaterialTheme.typography.bodySmall)
                },
                trailingContent = { Chevron() },
                modifier = Modifier.clickable {
                    if (node.isDirectory) {
                        navigator.push(node.name) { DirectoryScreen(node.file, navigator) }
                    } else {
                        navigator.push(node.name) { FileDetailScreen(node, navigator) }
                    }
                },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun FileDetailScreen(node: FileNode, navigator: ScizorNavigator) {
    val info = listOf(
        "Path" to node.path,
        "Name" to node.name,
        "Size" to FileBrowser.humanSize(node.size),
        "Type" to if (node.isDirectory) "Directory" else "File",
        "Modified" to formatDate(node.lastModified),
    )
    val content = FileBrowser.readText(node.file)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionHeader("Information")
        SegmentedColumn(items = info) { (label, value), shapes ->
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                supportingContent = { Text(value, style = MaterialTheme.typography.bodySmall) },
                content = { Text(label) },
            )
        }

        SectionHeader("Content")
        if (content == null) {
            Text(
                "Not a viewable text file, or too large to display.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
            )
        } else {
            SegmentedColumn(items = listOf("View file content")) { label, shapes ->
                SegmentedListItem(
                    onClick = { navigator.push(node.name) { TextReaderScreen(content) } },
                    shapes = shapes,
                    colors = scizorSegmentedColors(),
                    supportingContent = { Text("${content.length} chars") },
                    trailingContent = { Chevron() },
                    content = { Text(label) },
                )
            }
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
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatDate(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
