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

    fun record(packageName: String, title: String, text: String, time: Long) {
        val entry = LoggedNotification(counter++, packageName, title, text, time)
        _items.value = (listOf(entry) + _items.value).take(MAX)
    }

    fun clear() {
        _items.value = emptyList()
    }

    fun isEnabled(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}
