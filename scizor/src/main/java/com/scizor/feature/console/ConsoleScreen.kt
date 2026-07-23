@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.scizor.feature.console

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scizor.ui.rememberSearchQuery
import com.scizor.ui.EmptyState
import androidx.compose.material.icons.filled.Terminal
import com.scizor.ui.rememberTopBarAction

@Composable
internal fun ConsoleScreen(viewModel: ConsoleViewModel = viewModel()) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val autoScroll by viewModel.autoScroll.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Drive the app-bar search into the view model's filter.
    val query = rememberSearchQuery("Filter")
    LaunchedEffect(query) { viewModel.setQuery(query) }
    if (entries.isNotEmpty()) {
        rememberTopBarAction(Icons.Filled.Delete, "Clear", viewModel::clear)
    }

    LaunchedEffect(entries.size, autoScroll) {
        if (autoScroll && entries.isNotEmpty()) listState.animateScrollToItem(entries.lastIndex)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = filter.level == null,
                onClick = { viewModel.setLevel(null) },
                label = { Text("All") },
            )
            LogLevel.entries.forEach { level ->
                FilterChip(
                    selected = filter.level == level,
                    onClick = { viewModel.setLevel(level) },
                    label = { Text(level.name.take(1)) },
                )
            }
            FilterChip(
                selected = autoScroll,
                onClick = { viewModel.setAutoScroll(!autoScroll) },
                label = { Text("Auto-scroll") },
            )
        }

        if (entries.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Terminal,
                title = "No logs yet",
                description = "Live Logcat output will stream in here.",
            )
            return@Column
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
        ) {
            items(entries.size) { index ->
                LogRow(entries[index])
            }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    val clipboard = LocalClipboardManager.current
    var expanded by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val line = "${entry.time}  ${entry.level.letter}  ${entry.tag}: ${entry.message}"
    androidx.compose.foundation.layout.Box {
        Text(
            text = line,
            color = colorFor(entry.level),
            fontSize = 12.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { expanded = !expanded },
                    onLongClick = { menu = true },
                )
                .padding(vertical = 2.dp),
        )
        androidx.compose.material3.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Copy") },
                onClick = { clipboard.setText(AnnotatedString(entry.message)); menu = false },
            )
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Copy with timestamp") },
                onClick = { clipboard.setText(AnnotatedString(line)); menu = false },
            )
        }
    }
}

private fun colorFor(level: LogLevel): Color = when (level) {
    LogLevel.ERROR, LogLevel.ASSERT -> Color(0xFFD23B3B)
    LogLevel.WARN -> Color(0xFFE0A100)
    LogLevel.INFO -> Color(0xFF2E7D32)
    else -> Color.Unspecified
}
