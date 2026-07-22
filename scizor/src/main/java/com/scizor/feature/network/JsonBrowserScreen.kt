@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.scizor.feature.network

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scizor.ui.ScizorNavigator
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

private data class JsonRow(val label: String, val value: Any?)

/** A hierarchical JSON browser: objects and arrays drill in, leaves are copyable. */
@Composable
internal fun JsonBrowserScreen(json: String, navigator: ScizorNavigator) {
    val root = remember(json) { runCatching { JSONTokener(json).nextValue() }.getOrNull() }
    val rows = remember(root) { rowsFor(root) }

    if (rows == null) {
        // Not a JSON container — show the raw value.
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.TopStart) {
            Text(json, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(rows) { _, row ->
            JsonRowItem(row, navigator)
            HorizontalDivider()
        }
    }
}

private fun rowsFor(value: Any?): List<JsonRow>? = when (value) {
    is JSONObject -> value.keys().asSequence().map { JsonRow(it, value.get(it)) }.toList()
    is JSONArray -> (0 until value.length()).map { JsonRow("[$it]", value.get(it)) }
    else -> null
}

@Composable
private fun JsonRowItem(row: JsonRow, navigator: ScizorNavigator) {
    val clipboard = LocalClipboardManager.current
    when (val v = row.value) {
        is JSONObject -> ListItem(
            headlineContent = { Text(row.label) },
            supportingContent = { Text("{ ${v.length()} }") },
            trailingContent = { Chevron() },
            modifier = Modifier.combinedClickable(onClick = {
                navigator.push(row.label) { JsonBrowserScreen(v.toString(), navigator) }
            }, onLongClick = {}),
        )
        is JSONArray -> ListItem(
            headlineContent = { Text(row.label) },
            supportingContent = { Text("[ ${v.length()} ]") },
            trailingContent = { Chevron() },
            modifier = Modifier.combinedClickable(onClick = {
                navigator.push(row.label) { JsonBrowserScreen(v.toString(), navigator) }
            }, onLongClick = {}),
        )
        else -> ListItem(
            headlineContent = { Text(row.label) },
            supportingContent = {
                Text(v?.toString() ?: "null", maxLines = 3, overflow = TextOverflow.Ellipsis)
            },
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = { clipboard.setText(AnnotatedString("${row.label}: ${v?.toString() ?: "null"}")) },
            ),
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
