package com.scizor.feature.network

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scizor.ui.EmptyState
import com.scizor.ui.rememberSearchQuery
import com.scizor.ui.rememberTopBarAction
import com.scizor.ui.ScizorNavigator

/** Entry point: the transaction list; tapping a row pushes the detail page. */
@Composable
internal fun NetworkScreen(
    navigator: ScizorNavigator,
    viewModel: NetworkViewModel = viewModel(),
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    NetworkList(
        transactions = transactions,
        onSelect = { tx ->
            navigator.push("Request Details") { NetworkDetailScreen(tx, navigator) }
        },
        onClear = viewModel::clear,
    )
}

@Composable
private fun NetworkList(
    transactions: List<NetworkTransaction>,
    onSelect: (NetworkTransaction) -> Unit,
    onClear: () -> Unit,
) {
    val query = rememberSearchQuery("Search URL, method or status")
    if (transactions.isNotEmpty()) {
        rememberTopBarAction(Icons.Filled.Delete, "Clear all", onClear)
    }
    val filtered = transactions.filter {
        query.isBlank() ||
            it.url.contains(query, ignoreCase = true) ||
            it.method.contains(query, ignoreCase = true) ||
            it.status?.toString()?.contains(query) == true ||
            it.operationName?.contains(query, ignoreCase = true) == true
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (filtered.isEmpty()) {
            if (transactions.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.CompareArrows,
                    title = "No HTTP traffic yet",
                    description = "Add Scizor.network.interceptor() to your OkHttpClient, then make a request.",
                )
            } else {
                EmptyState(
                    icon = Icons.Filled.SearchOff,
                    title = "No matching requests",
                    description = "Try a different search term.",
                )
            }
            return
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filtered, key = { it.id }) { tx ->
                TransactionRow(tx = tx, onClick = { onSelect(tx) })
                HorizontalDivider()
            }
        }
    }
}

/** Mirrors Scyther's HTTPRequestView: a status-colored bar, method/code/duration, then the URL. */
@Composable
private fun TransactionRow(tx: NetworkTransaction, onClick: () -> Unit) {
    val statusColor = statusColor(tx)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(statusColor),
        )
        Column(
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp),
        ) {
            Text(
                text = tx.method,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = tx.error?.let { "ERR" } ?: tx.status?.toString() ?: "0",
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor,
            )
            Text(
                text = "${tx.durationMs ?: 0} ms",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            if (tx.isGraphQL && tx.operationName != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tx.operationName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    tx.operationType?.let { type ->
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = type.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .background(gqlColor(type), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                }
            }
            Text(
                text = tx.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun gqlColor(type: String): Color = when (type.lowercase()) {
    "query" -> Color(0xFF2E7D32)
    "mutation" -> Color(0xFFE0932F)
    "subscription" -> Color(0xFF9C27B0)
    else -> Color(0xFF9E9E9E)
}

private fun statusColor(tx: NetworkTransaction): Color = when {
    tx.error != null -> Color(0xFF9E9E9E)
    tx.status == null || tx.status < 1 -> Color(0xFF9E9E9E)
    tx.status < 100 -> Color(0xFF2196F3)
    tx.status < 200 -> Color(0xFFFF9800)
    tx.status < 300 -> Color(0xFF2E7D32)
    tx.status < 400 -> Color(0xFF9C27B0)
    tx.status < 600 -> Color(0xFFD23B3B)
    else -> Color(0xFF9E9E9E)
}
