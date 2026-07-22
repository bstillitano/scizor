@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.scizor.feature.cookies

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors

@Composable
internal fun CookiesScreen() {
    val cookies = remember { CookieBrowser.cookies() }

    if (cookies.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No cookies observed in captured traffic.\nMake requests through Scizor's interceptor first.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionHeader("Cookies")
        SegmentedColumn(items = cookies) { cookie, shapes ->
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                overlineContent = { Text(cookie.host) },
                leadingContent = {
                    Icon(
                        imageVector = if (cookie.sent) Icons.Filled.CallMade else Icons.Filled.CallReceived,
                        contentDescription = if (cookie.sent) "Sent" else "Received",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                supportingContent = {
                    Text(cookie.value, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                content = { Text(cookie.name) },
            )
        }
    }
}
