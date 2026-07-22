@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.scizor.feature.crashlogs

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.scizor.feature.network.TextReaderScreen
import com.scizor.ui.ScizorNavigator
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors
import java.text.DateFormat
import java.util.Date

@Composable
internal fun CrashLogsScreen(navigator: ScizorNavigator) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    val crashes = remember(refresh) { CrashLogger.crashes(context) }
    val filtered = crashes.filter {
        query.isBlank() || it.type.contains(query, true) || it.message.contains(query, true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search crashes") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            if (crashes.isNotEmpty()) {
                IconButton(onClick = { CrashLogger.clear(context); refresh++ }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Clear all")
                }
            }
        }
        TextButton(
            onClick = {
                CrashLogger.recordForDemo(context, RuntimeException("Recorded test crash from Scizor"))
                refresh++
            },
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            Text("Record test crash")
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (crashes.isEmpty()) "No crashes recorded.\nThat's a good thing!" else "No matching crashes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
            return
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filtered, key = { it.id }) { crash ->
                ListItem(
                    overlineContent = { Text("${formatDate(crash.timestamp)}  ·  ${crash.appVersion}") },
                    headlineContent = { Text(crash.type.substringAfterLast('.')) },
                    supportingContent = { if (crash.message.isNotBlank()) Text(crash.message, maxLines = 2) },
                    trailingContent = { Chevron() },
                    modifier = Modifier.clickable {
                        navigator.push("Crash Details") { CrashDetailScreen(crash, navigator) }
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun CrashDetailScreen(crash: CrashLog, navigator: ScizorNavigator) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionHeader("Exception")
        InfoGroup(
            listOf(
                "Type" to crash.type,
                "Message" to crash.message.ifBlank { "—" },
                "Thread" to crash.threadName.ifBlank { "—" },
                "Time" to formatDate(crash.timestamp),
            ),
        )

        SectionHeader("Environment")
        InfoGroup(
            listOf(
                "App version" to crash.appVersion,
                "OS version" to crash.osVersion,
                "Device" to crash.device,
            ),
        )

        SectionHeader("Stack Trace")
        SegmentedColumn(items = listOf("View stack trace")) { label, shapes ->
            SegmentedListItem(
                onClick = { navigator.push("Stack Trace") { TextReaderScreen(crash.stackTrace) } },
                shapes = shapes,
                colors = scizorSegmentedColors(),
                trailingContent = { Chevron() },
                content = { Text(label) },
            )
        }

        SectionHeader("Report")
        SegmentedColumn(items = listOf("Copy report", "Share report")) { label, shapes ->
            SegmentedListItem(
                onClick = {
                    if (label.startsWith("Copy")) {
                        clipboard.setText(AnnotatedString(crash.fullReport()))
                    } else {
                        runCatching {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, crash.fullReport())
                            }
                            context.startActivity(
                                Intent.createChooser(intent, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    }
                },
                shapes = shapes,
                colors = scizorSegmentedColors(),
                content = { Text(label, color = MaterialTheme.colorScheme.primary) },
            )
        }
    }
}

@Composable
private fun InfoGroup(rows: List<Pair<String, String>>) {
    SegmentedColumn(items = rows) { (label, value), shapes ->
        SegmentedListItem(
            shapes = shapes,
            colors = scizorSegmentedColors(),
            supportingContent = { Text(value, style = MaterialTheme.typography.bodySmall) },
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

private fun formatDate(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(Date(millis))
