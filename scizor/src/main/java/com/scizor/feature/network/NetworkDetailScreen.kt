@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.scizor.feature.network

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors

@Composable
internal fun NetworkDetailScreen(transaction: NetworkTransaction) {
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Overview
        val overview = buildList {
            add("URL" to transaction.url)
            add("Method" to transaction.method)
            add("Status" to (transaction.error?.let { "error: $it" } ?: transaction.status?.toString() ?: "—"))
            add("Duration" to (transaction.durationMs?.let { "$it ms" } ?: "—"))
            transaction.responseBody?.let { add("Response size" to "${it.toByteArray().size} bytes") }
        }
        SectionHeader("Overview")
        SegmentedColumn(items = overview) { (label, value), shapes ->
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                supportingContent = { Text(value) },
                modifier = Modifier.copyOnLongPress(label, value, clipboard),
                content = { Text(label) },
            )
        }

        // Request cURL
        SectionHeader("Request")
        SegmentedColumn(items = listOf("Copy as cURL")) { title, shapes ->
            SegmentedListItem(
                onClick = { clipboard.setText(AnnotatedString(transaction.toCurl())) },
                shapes = shapes,
                colors = scizorSegmentedColors(),
                content = { Text(title, color = MaterialTheme.colorScheme.primary) },
            )
        }

        HeadersSection("Request Headers", transaction.requestHeaders, "No headers sent")
        BodySection("Request Body", transaction.requestBody, "No content sent")
        HeadersSection("Response Headers", transaction.responseHeaders, "No headers received")
        BodySection("Response Body", transaction.responseBody, "No data received")
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
    val entries = headers.entries.sortedBy { it.key }
    SegmentedColumn(items = entries) { entry, shapes ->
        SegmentedListItem(
            shapes = shapes,
            colors = scizorSegmentedColors(),
            supportingContent = { Text(entry.value) },
            modifier = Modifier.copyOnLongPress(entry.key, entry.value, clipboard),
            content = { Text(entry.key) },
        )
    }
}

@Composable
private fun BodySection(title: String, body: String?, emptyText: String) {
    SectionHeader(title)
    if (body.isNullOrEmpty()) {
        EmptyRow(emptyText)
        return
    }
    Text(
        text = body,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 8.dp),
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

private fun Modifier.copyOnLongPress(
    label: String,
    value: String,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
): Modifier = this.combinedClickable(
    onClick = {},
    onLongClick = { clipboard.setText(AnnotatedString("$label: $value")) },
)
