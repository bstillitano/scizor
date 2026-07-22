package com.scizor.feature.notifications

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date

@Composable
internal fun NotificationLoggerScreen() {
    val context = LocalContext.current
    val items by NotificationLogger.items.collectAsStateWithLifecycle()
    val enabled = NotificationLogger.isEnabled(context)

    Column(modifier = Modifier.fillMaxSize()) {
        if (!enabled) {
            Text(
                "Notification access is required to log notifications.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
            Button(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text("Open notification access")
            }
        }

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No notifications logged yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            IconButton(onClick = { NotificationLogger.clear() }) {
                Icon(Icons.Filled.Delete, contentDescription = "Clear")
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items, key = { it.id }) { notification ->
                ListItem(
                    overlineContent = {
                        val time = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(notification.time))
                        Text("${notification.packageName}  ·  $time")
                    },
                    headlineContent = { Text(notification.title.ifBlank { "(no title)" }) },
                    supportingContent = {
                        if (notification.text.isNotBlank()) Text(notification.text)
                    },
                )
                HorizontalDivider()
            }
        }
    }
}
