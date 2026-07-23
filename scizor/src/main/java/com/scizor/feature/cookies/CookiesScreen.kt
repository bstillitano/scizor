@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.scizor.feature.cookies

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scizor.ui.EmptyState
import com.scizor.ui.ScizorNavigator
import com.scizor.ui.rememberSearchQuery
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors

@Composable
internal fun CookiesScreen(navigator: ScizorNavigator) {
    val all = remember { CookieBrowser.cookies() }
    val query = rememberSearchQuery("Search cookies")

    if (all.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Cookie,
            title = "No cookies yet",
            description = "Cookies from captured traffic appear here, or log your own with Scizor.cookies.log(...).",
        )
        return
    }

    val cookies = all.filter { c ->
        query.isBlank() || c.name.contains(query, true) || c.host.contains(query, true) ||
            c.value.contains(query, true)
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
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
                supportingContent = { Text(cookie.value, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                trailingContent = { Chevron() },
                modifier = Modifier.clickable {
                    navigator.push(cookie.name) { CookieDetailScreen(cookie) }
                },
                content = { Text(cookie.name) },
            )
        }
    }
}

@Composable
private fun CookieDetailScreen(cookie: Cookie) {
    val clipboard = LocalClipboardManager.current
    val rows = buildList {
        add("Name" to cookie.name)
        add("Value" to cookie.value)
        add("Host" to cookie.host)
        add("Direction" to if (cookie.sent) "Sent (request)" else "Received (response)")
        cookie.path?.let { add("Path" to it) }
        cookie.domain?.let { add("Domain" to it) }
        cookie.expires?.let { add("Expires" to it) }
        cookie.sameSite?.let { add("SameSite" to it) }
        add("HttpOnly" to cookie.httpOnly.toString())
        add("Secure" to cookie.secure.toString())
    }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionHeader("Cookie")
        SegmentedColumn(items = rows) { (label, value), shapes ->
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                supportingContent = { Text(value) },
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { clipboard.setText(AnnotatedString("$label: $value")) },
                ),
                content = { Text(label) },
            )
        }
    }
}

@Composable
private fun Chevron() {
    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
}
