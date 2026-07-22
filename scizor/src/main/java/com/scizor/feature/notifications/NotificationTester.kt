package com.scizor.feature.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Options controlling a test notification. */
internal data class TestOptions(
    val title: String,
    val body: String,
    val payload: String = "",
    val sound: Boolean = true,
    val delaySeconds: Int = 0,
    val repeatCount: Int = 1,
)

/** A queued test notification that has not fired yet. */
internal data class ScheduledNotification(
    val id: Int,
    val title: String,
    val fireAt: Long,
    val remaining: Int,
)

/** Posts local test notifications so notification UI can be verified without a backend. */
internal object NotificationTester {

    private const val CHANNEL_SOUND = "scizor_test"
    private const val CHANNEL_SILENT = "scizor_test_silent"

    private var nextId = 4000
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = mutableMapOf<Int, Job>()

    private val _scheduled = MutableStateFlow<List<ScheduledNotification>>(emptyList())
    val scheduled: StateFlow<List<ScheduledNotification>> = _scheduled.asStateFlow()

    /** Posts (or schedules) a notification per [options]. Returns the schedule id. */
    fun fire(context: Context, options: TestOptions): Int {
        ensureChannels(context)
        val id = nextId++
        val count = options.repeatCount.coerceAtLeast(1)
        val delayMs = options.delaySeconds.coerceAtLeast(0) * 1000L

        if (delayMs == 0L && count == 1) {
            post(context, id, options)
            return id
        }
        _scheduled.value = _scheduled.value + ScheduledNotification(
            id, options.title.ifBlank { "Scizor test" }, System.currentTimeMillis() + delayMs, count,
        )
        jobs[id] = scope.launch {
            if (delayMs > 0) delay(delayMs)
            var left = count
            while (left > 0) {
                post(context, nextId++, options)
                left--
                updateRemaining(id, left)
                if (left > 0) delay(maxOf(delayMs, 3000L))
            }
            complete(id)
        }
        return id
    }

    fun cancel(id: Int) {
        jobs.remove(id)?.cancel()
        _scheduled.value = _scheduled.value.filterNot { it.id == id }
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun post(context: Context, id: Int, options: TestOptions) {
        val channel = if (options.sound) CHANNEL_SOUND else CHANNEL_SILENT
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(options.title.ifBlank { "Scizor test" })
            .setContentText(options.body.ifBlank { "Test notification from Scizor" })
            .setAutoCancel(true)
        if (options.payload.isNotBlank()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText("${options.body}\n\n${options.payload}"))
            builder.extras.putString("scizor_payload", options.payload)
        }
        runCatching { NotificationManagerCompat.from(context).notify(id, builder.build()) }
    }

    private fun updateRemaining(id: Int, remaining: Int) {
        _scheduled.value = _scheduled.value.map { if (it.id == id) it.copy(remaining = remaining) else it }
    }

    private fun complete(id: Int) {
        jobs.remove(id)
        _scheduled.value = _scheduled.value.filterNot { it.id == id }
    }

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_SOUND, "Scizor Test", NotificationManager.IMPORTANCE_HIGH),
            )
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_SILENT, "Scizor Test (silent)", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }
}
