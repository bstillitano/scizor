package com.scizor.feature.console

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
internal fun ConsoleScreen(viewModel: ConsoleViewModel = viewModel()) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) listState.animateScrollToItem(entries.lastIndex)
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
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = filter.query,
                onValueChange = viewModel::setQuery,
                label = { Text("Filter") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = viewModel::clear) {
                Icon(Icons.Filled.Delete, contentDescription = "Clear")
            }
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
    Text(
        text = "${entry.time}  ${entry.level.letter}  ${entry.tag}: ${entry.message}",
        color = colorFor(entry.level),
        fontSize = 12.sp,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    )
}

private fun colorFor(level: LogLevel): Color = when (level) {
    LogLevel.ERROR, LogLevel.ASSERT -> Color(0xFFD23B3B)
    LogLevel.WARN -> Color(0xFFE0A100)
    LogLevel.INFO -> Color(0xFF2E7D32)
    else -> Color.Unspecified
}
