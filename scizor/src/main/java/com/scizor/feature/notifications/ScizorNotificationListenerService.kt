package com.scizor.feature.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Feeds posted notifications into [NotificationLogger]. Declared in the library
 * manifest (debug artifact only); the user must enable notification access.
 */
class ScizorNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        NotificationLogger.record(
            packageName = sbn.packageName,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
            time = sbn.postTime,
        )
    }
}
