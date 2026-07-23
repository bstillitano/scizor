@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.scizor.feature.keystore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.scizor.ui.ScizorNavigator
import com.scizor.ui.EmptyState
import androidx.compose.material.icons.filled.Key
import com.scizor.ui.rememberSearchQuery
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors
import java.text.DateFormat
import java.util.Date

@Composable
internal fun KeystoreScreen(navigator: ScizorNavigator) {
    val entries = remember { KeystoreBrowser.entries() }
    val query = rememberSearchQuery("Search aliases")

    if (entries.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Key,
            title = "No keystore entries",
            description = "This app has no keys or certificates in the AndroidKeyStore.",
        )
        return
    }

    val filtered = entries.filter { query.isBlank() || it.alias.contains(query, true) }
    val keys = filtered.filter { it.type == "Key" }
    val certs = filtered.filter { it.type != "Key" }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (keys.isNotEmpty()) {
            SectionHeader("Keys")
            EntryList(keys, navigator)
        }
        if (certs.isNotEmpty()) {
            SectionHeader("Certificates")
            EntryList(certs, navigator)
        }
    }
}

@Composable
private fun EntryList(entries: List<KeystoreEntry>, navigator: ScizorNavigator) {
    SegmentedColumn(items = entries) { entry, shapes ->
        val created = if (entry.created > 0) {
            DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(entry.created))
        } else {
            "—"
        }
        SegmentedListItem(
            onClick = { navigator.push(entry.alias) { KeystoreDetailScreen(entry.alias) } },
            shapes = shapes,
            colors = scizorSegmentedColors(),
            supportingContent = { Text("${entry.type}  ·  $created") },
            trailingContent = { Chevron() },
            content = { Text(entry.alias) },
        )
    }
}

@Composable
private fun KeystoreDetailScreen(alias: String) {
    val clipboard = LocalClipboardManager.current
    val rows = remember(alias) { KeystoreBrowser.details(alias) }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionHeader("Entry")
        SegmentedColumn(items = rows) { (label, value), shapes ->
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
    }
}

@Composable
private fun Chevron() {
    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
}
