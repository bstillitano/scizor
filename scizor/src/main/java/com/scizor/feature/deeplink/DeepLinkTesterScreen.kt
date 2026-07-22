@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.scizor.feature.deeplink

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors
import java.text.DateFormat
import java.util.Date

@Composable
internal fun DeepLinkTesterScreen() {
    val context = LocalContext.current
    val history by DeepLinkTester.history.collectAsStateWithLifecycle()
    var url by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionHeader("Test URL")
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Enter URL (e.g. myapp://home)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Button(
            onClick = { DeepLinkTester.fire(context, url) },
            enabled = url.isNotBlank(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Text("Open URL")
        }

        if (history.isNotEmpty()) {
            SectionHeader("History")
            SegmentedColumn(items = history) { entry, shapes ->
                SegmentedListItem(
                    onClick = { url = entry.url },
                    shapes = shapes,
                    colors = scizorSegmentedColors(),
                    leadingContent = {
                        if (entry.success) {
                            Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF2E7D32))
                        } else {
                            Icon(Icons.Filled.Cancel, null, tint = Color(0xFFD23B3B))
                        }
                    },
                    supportingContent = {
                        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(entry.timestamp))
                        Text(entry.error?.let { "$it  ·  $time" } ?: time)
                    },
                    content = { Text(entry.url, maxLines = 1) },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { DeepLinkTester.clearHistory() }) {
                    Text("Clear history", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
