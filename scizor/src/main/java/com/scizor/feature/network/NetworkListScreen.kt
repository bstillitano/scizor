package com.scizor.feature.network

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/** Entry point: shows the transaction list, or a selected transaction's detail. */
@Composable
internal fun NetworkScreen(viewModel: NetworkViewModel = viewModel()) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    var selectedId by remember { mutableStateOf<Long?>(null) }

    val selected = selectedId?.let { id -> transactions.firstOrNull { it.id == id } }
    if (selected != null) {
        BackHandler { selectedId = null }
        NetworkDetailScreen(transaction = selected)
    } else {
        NetworkList(
            transactions = transactions,
            onSelect = { selectedId = it.id },
            onClear = viewModel::clear,
        )
    }
}

@Composable
private fun NetworkList(
    transactions: List<NetworkTransaction>,
    onSelect: (NetworkTransaction) -> Unit,
    onClear: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = transactions.filter {
        query.isBlank() ||
            it.url.contains(query, ignoreCase = true) ||
            it.method.contains(query, ignoreCase = true) ||
            it.status?.toString()?.contains(query) == true
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search URL, method or status") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClear) {
                Icon(Icons.Filled.Delete, contentDescription = "Clear all")
            }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (transactions.isEmpty()) {
                        "No HTTP traffic captured yet.\nAdd Scizor.network.interceptor() to your OkHttpClient."
                    } else {
                        "No requests match your search."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
            return
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filtered, key = { it.id }) { tx ->
                ListItem(
                    headlineContent = { Text("${tx.method}  ${tx.path}") },
                    supportingContent = {
                        val timing = tx.durationMs?.let { "  ·  ${it} ms" } ?: ""
                        Text("${tx.host}$timing")
                    },
                    trailingContent = { StatusLabel(tx) },
                    modifier = Modifier.clickable { onSelect(tx) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun StatusLabel(tx: NetworkTransaction) {
    val text = tx.error?.let { "ERR" } ?: tx.status?.toString() ?: "…"
    val color = when {
        tx.error != null -> Color(0xFFD23B3B)
        tx.status == null -> Color.Unspecified
        tx.status in 200..299 -> Color(0xFF2E7D32)
        tx.status in 400..599 -> Color(0xFFD23B3B)
        else -> Color(0xFFE0A100)
    }
    Text(text = text, color = color, style = MaterialTheme.typography.labelLarge)
}
