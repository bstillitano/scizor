@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.scizor.feature.notifications

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scizor.ui.rememberSearchQuery
import com.scizor.ui.rememberTopBarAction
import com.scizor.ui.rememberTopBarSubtitle
import com.scizor.ui.ScizorNavigator
import com.scizor.ui.EmptyState
import androidx.compose.material.icons.filled.NotificationsNone
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors
import java.text.DateFormat
import java.util.Date

@Composable
internal fun NotificationLoggerScreen(navigator: ScizorNavigator) {
    val context = LocalContext.current
    val all by NotificationLogger.items.collectAsStateWithLifecycle()
    val enabled = NotificationLogger.isEnabled(context)
    val query = rememberSearchQuery("Search notifications")

    rememberTopBarSubtitle(if (enabled) "Access granted" else "Access required")

    if (enabled && all.isNotEmpty()) {
        rememberTopBarAction(Icons.Filled.Delete, "Clear") { NotificationLogger.clear() }
    } else {
        rememberTopBarAction(Icons.Filled.Settings, "Notification access") {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    val items = all.filter { n ->
        query.isBlank() || n.title.contains(query, true) || n.text.contains(query, true) ||
            n.packageName.contains(query, true)
    }

    if (!enabled) {
        EmptyState(
            icon = Icons.Filled.NotificationsOff,
            title = "Notification access required",
            description = "Grant access from the settings icon above to start logging notifications.",
        )
        return
    }

    if (items.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.NotificationsNone,
            title = "No notifications logged",
            description = "Notifications posted while access is granted will appear here.",
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionHeader("Logged (${items.size})")
        SegmentedColumn(items = items) { notification, shapes ->
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                overlineContent = {
                    val time = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(notification.time))
                    Text("${notification.packageName}  ·  $time")
                },
                supportingContent = {
                    if (notification.text.isNotBlank()) {
                        Text(notification.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                trailingContent = { Chevron() },
                modifier = Modifier.clickable {
                    navigator.push("Notification") { NotificationDetailScreen(notification.id) }
                },
                content = { Text(notification.title.ifBlank { "(no title)" }) },
            )
        }
    }
}

@Composable
private fun NotificationDetailScreen(id: Long) {
    val clipboard = LocalClipboardManager.current
    val notification = remember(id) { NotificationLogger.byId(id) } ?: return

    val fields = buildList {
        add("Package" to notification.packageName)
        add("Title" to notification.title.ifBlank { "(none)" })
        add("Text" to notification.text.ifBlank { "(none)" })
        notification.subText?.let { add("Sub-text" to it) }
        notification.channelId?.let { add("Channel" to it) }
        notification.category?.let { add("Category" to it) }
        notification.priority?.let { add("Priority" to it) }
        notification.group?.let { add("Group" to it) }
        add("Ongoing" to notification.ongoing.toString())
        add("Posted" to DateFormat.getDateTimeInstance().format(Date(notification.time)))
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionHeader("Details")
        SegmentedColumn(items = fields) { (label, value), shapes ->
            SegmentedListItem(
                shapes = shapes,
                colors = scizorSegmentedColors(),
                supportingContent = { Text(value, style = MaterialTheme.typography.bodyMedium) },
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { clipboard.setText(AnnotatedString("$label: $value")) },
                ),
                content = { Text(label, color = MaterialTheme.colorScheme.primary) },
            )
        }

        if (notification.actions.isNotEmpty()) {
            SectionHeader("Actions")
            SegmentedColumn(items = notification.actions) { action, shapes ->
                SegmentedListItem(shapes = shapes, colors = scizorSegmentedColors(), content = { Text(action) })
            }
        }

        if (notification.extras.isNotEmpty()) {
            SectionHeader("Raw extras")
            SegmentedColumn(items = notification.extras) { (key, value), shapes ->
                SegmentedListItem(
                    shapes = shapes,
                    colors = scizorSegmentedColors(),
                    supportingContent = { Text(value, style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = { clipboard.setText(AnnotatedString("$key: $value")) },
                    ),
                    content = { Text(key, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
    }
}

@Composable
private fun Chevron() {
    Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
