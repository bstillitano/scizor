@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.scizor.feature.deeplink

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors
import java.text.DateFormat
import java.util.Date

private val Success = Color(0xFF2E7D32)
private val Failure = Color(0xFFD23B3B)

@Composable
internal fun DeepLinkTesterScreen() {
    val context = LocalContext.current
    val history by DeepLinkTester.history.collectAsStateWithLifecycle()
    val presets = remember { DeepLinkTester.presets }
    var url by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    fun fire(target: String) {
        val ok = DeepLinkTester.fire(context, target)
        status = ok to if (ok) "Opened $target" else "Could not open $target"
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionHeader("Test URL")
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Enter URL (e.g. myapp://home)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Button(
            onClick = { fire(url) },
            enabled = url.isNotBlank(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        ) { Text("Open URL") }

        status?.let { (ok, message) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Icon(
                    if (ok) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                    null,
                    tint = if (ok) Success else Failure,
                )
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ok) Success else Failure,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        if (presets.isNotEmpty()) {
            SectionHeader("Presets")
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.forEach { preset ->
                    AssistChip(
                        onClick = { url = preset.url; fire(preset.url) },
                        label = { Text(preset.name) },
                    )
                }
            }
        }

        if (history.isNotEmpty()) {
            SectionHeader("History")
            SegmentedColumn(items = history) { entry, shapes ->
                var menuOpen by remember { mutableStateOf(false) }
                SegmentedListItem(
                    shapes = shapes,
                    colors = scizorSegmentedColors(),
                    leadingContent = {
                        Icon(
                            if (entry.success) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                            null,
                            tint = if (entry.success) Success else Failure,
                        )
                    },
                    supportingContent = {
                        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(entry.timestamp))
                        Text(entry.error?.let { "$it  ·  $time" } ?: time)
                    },
                    trailingContent = {
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Re-fire") }, onClick = { menuOpen = false; fire(entry.url) })
                            DropdownMenuItem(text = { Text("Copy to editor") }, onClick = { menuOpen = false; url = entry.url })
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = { menuOpen = false; DeepLinkTester.removeHistory(entry) },
                            )
                        }
                    },
                    modifier = Modifier.combinedClickable(
                        onClick = { url = entry.url },
                        onLongClick = { menuOpen = true },
                    ),
                    content = { Text(entry.url, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { DeepLinkTester.clearHistory() }) {
                    Text("Clear history", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
