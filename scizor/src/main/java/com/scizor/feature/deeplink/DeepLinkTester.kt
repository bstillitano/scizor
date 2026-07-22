package com.scizor.feature.deeplink

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.scizor.Scizor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** A record of a fired deep link. */
internal data class DeepLinkHistoryEntry(
    val url: String,
    val timestamp: Long,
    val success: Boolean,
    val error: String?,
)

/**
 * Fires deep links / custom URL schemes via an ACTION_VIEW intent and keeps a
 * persisted history of the results. Mirrors Scyther's Deep Link Tester.
 */
internal object DeepLinkTester {

    private const val MAX_HISTORY = 50
    private const val HISTORY_KEY = "deeplink_history"

    private val _history = MutableStateFlow<List<DeepLinkHistoryEntry>>(emptyList())
    val history: StateFlow<List<DeepLinkHistoryEntry>> = _history.asStateFlow()

    /** Host-registered presets, surfaced above the history in the tester. */
    val presets: List<DeepLinkPreset> get() = Scizor.deepLinkPresets

    /** Loads persisted history. Called once from [Scizor.start]. */
    fun init() {
        val json = Scizor.storeOrNull()?.string(HISTORY_KEY) ?: return
        _history.value = runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                DeepLinkHistoryEntry(
                    url = o.getString("url"),
                    timestamp = o.getLong("ts"),
                    success = o.getBoolean("ok"),
                    error = o.optString("err").ifBlank { null },
                )
            }
        }.getOrDefault(emptyList())
    }

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
        persist()
    }

    fun removeHistory(entry: DeepLinkHistoryEntry) {
        _history.value = _history.value - entry
        persist()
    }

    private fun record(url: String, success: Boolean, error: String?) {
        val entry = DeepLinkHistoryEntry(url, System.currentTimeMillis(), success, error)
        _history.value = (listOf(entry) + _history.value).take(MAX_HISTORY)
        persist()
    }

    private fun persist() {
        val array = JSONArray()
        _history.value.forEach { entry ->
            array.put(
                JSONObject()
                    .put("url", entry.url)
                    .put("ts", entry.timestamp)
                    .put("ok", entry.success)
                    .put("err", entry.error ?: ""),
            )
        }
        Scizor.storeOrNull()?.putString(HISTORY_KEY, array.toString())
    }
}
