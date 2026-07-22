package com.scizor.feature.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A notification observed by the listener service. */
internal data class LoggedNotification(
    val id: Long,
    val packageName: String,
    val title: String,
    val text: String,
    val time: Long,
    val subText: String? = null,
    val channelId: String? = null,
    val category: String? = null,
    val priority: String? = null,
    val group: String? = null,
    val ongoing: Boolean = false,
    val actions: List<String> = emptyList(),
    val extras: List<Pair<String, String>> = emptyList(),
)

/**
 * Buffers notifications reported by [ScizorNotificationListenerService]. Requires
 * the user to grant notification access (a system setting).
 */
internal object NotificationLogger {

    private const val MAX = 500

    private val _items = MutableStateFlow<List<LoggedNotification>>(emptyList())
    val items: StateFlow<List<LoggedNotification>> = _items.asStateFlow()

    private var counter = 0L

    fun record(notification: LoggedNotification) {
        _items.value = (listOf(notification.copy(id = counter++)) + _items.value).take(MAX)
    }

    fun byId(id: Long): LoggedNotification? = _items.value.firstOrNull { it.id == id }

    fun clear() {
        _items.value = emptyList()
    }

    fun isEnabled(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}
