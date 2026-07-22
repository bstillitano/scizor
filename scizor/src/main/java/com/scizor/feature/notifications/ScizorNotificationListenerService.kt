package com.scizor.feature.notifications

import android.app.Notification
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Feeds posted notifications into [NotificationLogger]. Declared in the library
 * manifest (debug artifact only); the user must enable notification access.
 */
class ScizorNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val extras = notification.extras
        NotificationLogger.record(
            LoggedNotification(
                id = 0,
                packageName = sbn.packageName,
                title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
                text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
                time = sbn.postTime,
                subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.ifBlank { null },
                channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) notification.channelId else null,
                category = notification.category,
                priority = priorityLabel(notification.priority),
                group = notification.group,
                ongoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0,
                actions = notification.actions?.mapNotNull { it.title?.toString() } ?: emptyList(),
                extras = extras.keySet().mapNotNull { key ->
                    val value = runCatching { extras.get(key)?.toString() }.getOrNull() ?: return@mapNotNull null
                    key to value.take(500)
                },
            ),
        )
    }

    private fun priorityLabel(priority: Int): String = when (priority) {
        Notification.PRIORITY_MAX -> "Max"
        Notification.PRIORITY_HIGH -> "High"
        Notification.PRIORITY_DEFAULT -> "Default"
        Notification.PRIORITY_LOW -> "Low"
        Notification.PRIORITY_MIN -> "Min"
        else -> priority.toString()
    }
}
