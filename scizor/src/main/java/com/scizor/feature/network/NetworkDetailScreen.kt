package com.scizor.feature.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

@Composable
internal fun NetworkDetailScreen(transaction: NetworkTransaction) {
    var tab by remember { mutableIntStateOf(0) }
    val clipboard = LocalClipboardManager.current
    val tabs = listOf("Overview", "Request", "Response")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            when (tab) {
                0 -> OverviewTab(transaction) {
                    clipboard.setText(AnnotatedString(transaction.toCurl()))
                }
                1 -> HeadersAndBody(transaction.requestHeaders, transaction.requestBody)
                else -> HeadersAndBody(transaction.responseHeaders, transaction.responseBody)
            }
        }
    }
}

@Composable
private fun OverviewTab(tx: NetworkTransaction, onCopyCurl: () -> Unit) {
    Field("URL", tx.url)
    Field("Method", tx.method)
    Field("Status", tx.error?.let { "error: $it" } ?: tx.status?.toString() ?: "—")
    Field("Duration", tx.durationMs?.let { "$it ms" } ?: "—")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        Button(onClick = onCopyCurl) { Text("Copy as cURL") }
    }
}

@Composable
private fun HeadersAndBody(headers: Map<String, String>, body: String?) {
    Text("Headers", style = MaterialTheme.typography.titleSmall)
    if (headers.isEmpty()) {
        Text("—", style = MaterialTheme.typography.bodySmall)
    } else {
        headers.forEach { (name, value) -> Field(name, value) }
    }
    Text(
        "Body",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 16.dp),
    )
    Text(
        text = body?.ifEmpty { "(empty)" } ?: "(none)",
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun Field(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
