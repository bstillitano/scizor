@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.scizor.feature.crashlogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val crashes = remember(refresh) { CrashLogger.crashes(context) }

    if (crashes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No crashes recorded.\nThat's a good thing!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            IconButton(onClick = {
                CrashLogger.clear(context)
                refresh++
            }) {
                Icon(Icons.Filled.Delete, contentDescription = "Clear all")
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(crashes, key = { it.id }) { crash ->
                ListItem(
                    overlineContent = { Text(formatDate(crash.timestamp)) },
                    headlineContent = { Text(crash.type.substringAfterLast('.')) },
                    supportingContent = {
                        if (crash.message.isNotBlank()) Text(crash.message, maxLines = 2)
                    },
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
    val info = listOf(
        "Type" to crash.type,
        "Message" to crash.message.ifBlank { "—" },
        "Thread" to crash.threadName.ifBlank { "—" },
        "Time" to formatDate(crash.timestamp),
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionHeader("Exception")
        SegmentedColumn(items = info) { (label, value), shapes ->
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                supportingContent = { Text(value, style = MaterialTheme.typography.bodySmall) },
                content = { Text(label) },
            )
        }

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
