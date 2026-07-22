@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.scizor.feature.network

import android.content.Intent
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scizor.ui.ScizorNavigator
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed interface Row {
    data class Value(val label: String, val value: String) : Row
    data class Link(val label: String, val preview: String, val onOpen: () -> Unit) : Row
}

@Composable
internal fun NetworkDetailScreen(transaction: NetworkTransaction, navigator: ScizorNavigator) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val timeFmt = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Overview
        val overview = buildList {
            add(Row.Link("URL", transaction.url) {
                navigator.push("Request URL") { TextReaderScreen(transaction.url) }
            })
            add(Row.Value("Method", transaction.method))
            add(Row.Value("Status", transaction.error?.let { "error: $it" } ?: transaction.status?.toString() ?: "—"))
            add(Row.Value("Response size", "${transaction.responseBytes ?: 0} bytes"))
            add(Row.Value("Duration", transaction.durationMs?.let { "$it ms" } ?: "—"))
            add(Row.Value("Date", timeFmt.format(Date(transaction.timestamp))))
        }
        SectionHeader("Overview")
        RowGroup(overview)

        // GraphQL
        if (transaction.isGraphQL) {
            SectionHeader("GraphQL")
            val gql = buildList {
                add(Row.Value("Operation", transaction.operationName ?: "—"))
                add(Row.Value("Type", transaction.operationType ?: "—"))
                transaction.variables?.let { vars ->
                    add(Row.Link("Browse variables", vars) {
                        navigator.push("Variables") { JsonBrowserScreen(vars, navigator) }
                    })
                }
            }
            RowGroup(gql)
        }

        // Request cURL
        SectionHeader("Request")
        SegmentedColumn(items = listOf("Copy as cURL", "Share cURL")) { title, shapes ->
            SegmentedListItem(
                onClick = {
                    val curl = transaction.toCurl()
                    if (title.startsWith("Copy")) clipboard.setText(AnnotatedString(curl)) else shareText(context, curl)
                },
                shapes = shapes,
                colors = scizorSegmentedColors(),
                content = { Text(title, color = MaterialTheme.colorScheme.primary) },
            )
        }

        HeadersSection("Request Headers", transaction.requestHeaders, "No headers sent")
        BodySection("Request Body", transaction.requestBody, transaction.contentType, "No content sent", navigator)
        HeadersSection("Response Headers", transaction.responseHeaders, "No headers received")
        BodySection("Response Body", transaction.responseBody, transaction.contentType, "No data received", navigator)

        // Developer Info
        SectionHeader("Developer Info")
        val dev = buildList {
            add(Row.Value("Request time", timeFmt.format(Date(transaction.timestamp))))
            add(Row.Value("Response time", timeFmt.format(Date(transaction.timestamp + (transaction.durationMs ?: 0)))))
            add(Row.Value("Cache policy", transaction.cacheControl?.ifBlank { "—" } ?: "—"))
            add(Row.Value("Content-Type", transaction.contentType ?: "—"))
            add(Row.Value("Timeout", transaction.timeoutMs?.let { "$it ms" } ?: "—"))
        }
        RowGroup(dev)
    }
}

@Composable
private fun RowGroup(rows: List<Row>) {
    val clipboard = LocalClipboardManager.current
    SegmentedColumn(items = rows) { row, shapes ->
        when (row) {
            is Row.Value -> SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                trailingContent = {
                    Text(row.value, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { clipboard.setText(AnnotatedString("${row.label}: ${row.value}")) },
                ),
                content = { Text(row.label) },
            )
            is Row.Link -> SegmentedListItem(
                onClick = row.onOpen,
                shapes = shapes,
                colors = scizorSegmentedColors(),
                supportingContent = { Text(row.preview, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                trailingContent = { Chevron() },
                content = { Text(row.label) },
            )
        }
    }
}

@Composable
private fun HeadersSection(title: String, headers: Map<String, String>, emptyText: String) {
    SectionHeader(title)
    if (headers.isEmpty()) {
        EmptyRow(emptyText)
        return
    }
    val clipboard = LocalClipboardManager.current
    SegmentedColumn(items = headers.entries.sortedBy { it.key }) { entry, shapes ->
        SegmentedListItem(
            shapes = shapes,
            colors = scizorSegmentedColors(),
            supportingContent = { Text(entry.value) },
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = { clipboard.setText(AnnotatedString("${entry.key}: ${entry.value}")) },
            ),
            content = { Text(entry.key) },
        )
    }
}

@Composable
private fun BodySection(
    title: String,
    body: String?,
    contentType: String?,
    emptyText: String,
    navigator: ScizorNavigator,
) {
    SectionHeader(title)
    if (body.isNullOrEmpty()) {
        EmptyRow(emptyText)
        return
    }
    val isJson = Json.looksLikeJson(contentType, body)
    val links = buildList {
        add("View ${title.lowercase()}")
        if (isJson) add("Browse ${title.lowercase()}")
    }
    SegmentedColumn(items = links) { label, shapes ->
        SegmentedListItem(
            onClick = {
                if (label.startsWith("Browse")) {
                    navigator.push(title) { JsonBrowserScreen(body, navigator) }
                } else {
                    val text = if (isJson) Json.pretty(body) else body
                    navigator.push(title) { TextReaderScreen(text) }
                }
            },
            shapes = shapes,
            colors = scizorSegmentedColors(),
            supportingContent = { Text("${body.length} chars") },
            trailingContent = { Chevron() },
            content = { Text(label) },
        )
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
private fun EmptyRow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
    )
}

private fun shareText(context: android.content.Context, text: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
