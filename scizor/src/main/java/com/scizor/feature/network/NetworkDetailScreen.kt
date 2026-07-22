@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.scizor.feature.network

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scizor.ui.ScizorNavigator
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors
import java.text.DateFormat
import java.util.Date

private sealed interface OverviewRow {
    data class Value(val label: String, val value: String) : OverviewRow
    data class Link(val label: String, val preview: String, val onOpen: () -> Unit) : OverviewRow
}

@Composable
internal fun NetworkDetailScreen(transaction: NetworkTransaction, navigator: ScizorNavigator) {
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        val overview = buildList {
            add(
                OverviewRow.Link("URL", transaction.url) {
                    navigator.push("Request URL") { TextReaderScreen(transaction.url) }
                },
            )
            add(OverviewRow.Value("Method", transaction.method))
            add(
                OverviewRow.Value(
                    "Status",
                    transaction.error?.let { "error: $it" } ?: transaction.status?.toString() ?: "—",
                ),
            )
            add(OverviewRow.Value("Duration", transaction.durationMs?.let { "$it ms" } ?: "—"))
            transaction.responseBody?.let {
                add(OverviewRow.Value("Response size", "${it.toByteArray().size} bytes"))
            }
            add(
                OverviewRow.Value(
                    "Date",
                    DateFormat.getDateTimeInstance().format(Date(transaction.timestamp)),
                ),
            )
        }

        SectionHeader("Overview")
        SegmentedColumn(items = overview) { row, shapes ->
            when (row) {
                is OverviewRow.Value -> SegmentedListItem(
                    shapes = shapes,
                    colors = scizorSegmentedColors(),
                    trailingContent = {
                        Text(
                            text = row.value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    content = { Text(row.label) },
                )
                is OverviewRow.Link -> SegmentedListItem(
                    onClick = row.onOpen,
                    shapes = shapes,
                    colors = scizorSegmentedColors(),
                    supportingContent = {
                        Text(row.preview, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    trailingContent = { Chevron() },
                    content = { Text(row.label) },
                )
            }
        }

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
        BodySection("Request Body", transaction.requestBody, "No content sent", navigator)
        HeadersSection("Response Headers", transaction.responseHeaders, "No headers received")
        BodySection("Response Body", transaction.responseBody, "No data received", navigator)
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
    emptyText: String,
    navigator: ScizorNavigator,
) {
    SectionHeader(title)
    if (body.isNullOrEmpty()) {
        EmptyRow(emptyText)
        return
    }
    SegmentedColumn(items = listOf("View ${title.lowercase()}")) { label, shapes ->
        SegmentedListItem(
            onClick = { navigator.push(title) { TextReaderScreen(body) } },
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
