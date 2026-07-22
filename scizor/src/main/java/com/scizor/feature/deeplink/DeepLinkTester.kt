package com.scizor.feature.deeplink

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A record of a fired deep link. */
internal data class DeepLinkHistoryEntry(
    val url: String,
    val timestamp: Long,
    val success: Boolean,
    val error: String?,
)

/**
 * Fires deep links / custom URL schemes via an ACTION_VIEW intent and keeps a
 * session history of the results. Mirrors Scyther's Deep Link Tester.
 */
internal object DeepLinkTester {

    private const val MAX_HISTORY = 50

    private val _history = MutableStateFlow<List<DeepLinkHistoryEntry>>(emptyList())
    val history: StateFlow<List<DeepLinkHistoryEntry>> = _history.asStateFlow()

    /** Attempts to open [url]; records the outcome in history and returns success. */
    fun fire(context: Context, url: String): Boolean {
        val trimmed = url.trim()
        val uri = trimmed.takeIf { it.isNotEmpty() }?.let { runCatching { Uri.parse(it) }.getOrNull() }
        if (uri == null) {
            record(trimmed, success = false, error = "Invalid URL format")
            return false
        }
        val result = runCatching {
            val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
        return if (result.isSuccess) {
            record(trimmed, success = true, error = null)
            true
        } else {
            record(trimmed, success = false, error = "This URL cannot be opened")
            false
        }
    }

    fun clearHistory() {
        _history.value = emptyList()
    }

    fun removeHistory(entry: DeepLinkHistoryEntry) {
        _history.value = _history.value - entry
    }

    private fun record(url: String, success: Boolean, error: String?) {
        val entry = DeepLinkHistoryEntry(url, System.currentTimeMillis(), success, error)
        _history.value = (listOf(entry) + _history.value).take(MAX_HISTORY)
    }
}
