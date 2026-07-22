@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.scizor.feature.filebrowser

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.scizor.feature.network.Json
import com.scizor.feature.network.TextReaderScreen
import com.scizor.ui.rememberSearchQuery
import com.scizor.ui.ScizorNavigator
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentInset
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
internal fun FileBrowserScreen(navigator: ScizorNavigator) {
    val context = LocalContext.current
    val query = rememberSearchQuery("Search locations")
    val roots = FileBrowser.roots(context).filter {
        query.isBlank() || it.label.contains(query, true) || it.path.contains(query, true)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = SegmentInset),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        itemsIndexed(roots, key = { _, it -> it.path }) { index, root ->
            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(index = index, count = roots.size),
                colors = scizorSegmentedColors(),
                leadingContent = { Icon(Icons.Filled.Folder, null, tint = MaterialTheme.colorScheme.primary) },
                supportingContent = { Text(root.path, style = MaterialTheme.typography.bodySmall) },
                trailingContent = { Chevron() },
                modifier = Modifier.combinedClickable(
                    onClick = { navigator.push(root.label) { DirectoryScreen(File(root.path), navigator) } },
                    onLongClick = {},
                ),
                content = { Text(root.label) },
            )
        }
    }
}

@Composable
private fun DirectoryScreen(dir: File, navigator: ScizorNavigator) {
    var refresh by remember { mutableIntStateOf(0) }
    val query = rememberSearchQuery("Search this folder")
    val all = remember(refresh) { FileBrowser.list(dir) }
    val nodes = all.filter { query.isBlank() || it.name.contains(query, true) }
    if (nodes.isEmpty()) {
        EmptyState(if (all.isEmpty()) "Empty folder" else "No files match.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = SegmentInset),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        itemsIndexed(nodes, key = { _, it -> it.path }) { index, node ->
            var menu by remember { mutableStateOf(false) }
            Box {
                SegmentedListItem(
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = nodes.size),
                    colors = scizorSegmentedColors(),
                    leadingContent = { Icon(iconFor(node.kind), null, tint = iconTint(node)) },
                    supportingContent = {
                        val sizePart = FileBrowser.humanSize(node.size)
                        Text("$sizePart  ·  ${formatDate(node.lastModified)}", style = MaterialTheme.typography.bodySmall)
                    },
                    trailingContent = { Chevron() },
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            if (node.isDirectory) {
                                navigator.push(node.name) { DirectoryScreen(node.file, navigator) }
                            } else {
                                navigator.push(node.name) { FileDetailScreen(node, navigator) { refresh++ } }
                            }
                        },
                        onLongClick = { menu = true },
                    ),
                    content = { Text(node.name) },
                )
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Delete") }, onClick = {
                        FileBrowser.delete(node.file); menu = false; refresh++
                    })
                }
            }
        }
    }
}

@Composable
private fun FileDetailScreen(node: FileNode, navigator: ScizorNavigator, onChanged: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val info = listOf(
        "Path" to node.path,
        "Name" to node.name,
        "Size" to FileBrowser.humanSize(node.size),
        "Type" to node.kind.name.lowercase().replaceFirstChar { it.uppercase() },
        "Modified" to formatDate(node.lastModified),
        "Permissions" to FileBrowser.permissions(node.file),
    )
    val rawContent = remember(node.path) { FileBrowser.readText(node.file) }
    val content = remember(rawContent) {
        if (node.kind == FileKind.JSON && rawContent != null) Json.pretty(rawContent) else rawContent
    }
    val image = remember(node.path) {
        if (node.kind == FileKind.IMAGE) {
            runCatching { BitmapFactory.decodeFile(node.path)?.asImageBitmap() }.getOrNull()
        } else {
            null
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionHeader("Information")
        SegmentedColumn(items = info) { (label, value), shapes ->
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                supportingContent = { Text(value, style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { clipboard.setText(AnnotatedString("$label: $value")) },
                ),
                content = { Text(label) },
            )
        }

        SectionHeader("Actions")
        val actions = buildList {
            add("Copy path")
            if (node.file.canRead()) add("Share")
            add("Delete")
        }
        SegmentedColumn(items = actions) { action, shapes ->
            SegmentedListItem(
                onClick = {
                    when (action) {
                        "Copy path" -> clipboard.setText(AnnotatedString(node.path))
                        "Share" -> shareFile(context, node.file)
                        "Delete" -> { FileBrowser.delete(node.file); onChanged(); navigator.pop() }
                    }
                },
                shapes = shapes,
                colors = scizorSegmentedColors(),
                content = {
                    Text(action, color = if (action == "Delete") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                },
            )
        }

        if (image != null) {
            SectionHeader("Preview")
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).padding(16.dp),
            )
        }

        SectionHeader("Content")
        if (content == null) {
            Text(
                "Not a viewable text file.",
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

private fun iconFor(kind: FileKind): ImageVector = when (kind) {
    FileKind.DIRECTORY -> Icons.Filled.Folder
    FileKind.IMAGE -> Icons.Filled.Image
    FileKind.TEXT, FileKind.JSON -> Icons.Filled.Description
    FileKind.DATABASE -> Icons.Filled.Storage
    FileKind.ARCHIVE -> Icons.Filled.FolderZip
    FileKind.AUDIO -> Icons.Filled.AudioFile
    FileKind.VIDEO -> Icons.Filled.VideoFile
    FileKind.PDF -> Icons.Filled.PictureAsPdf
    FileKind.BINARY -> Icons.AutoMirrored.Filled.InsertDriveFile
}

@Composable
private fun iconTint(node: FileNode) =
    if (node.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

@Composable
private fun Chevron() {
    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatDate(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

private fun shareFile(context: android.content.Context, file: File) {
    // Share the text content (avoids needing a FileProvider for arbitrary files).
    runCatching {
        val text = FileBrowser.readText(file) ?: file.absolutePath
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
