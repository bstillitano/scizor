package com.scizor.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

private val Context.scizorDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "scizor_settings",
)

/**
 * Thin persistence layer over Jetpack DataStore.
 *
 * All keys are namespaced with [PREFIX] (`scizor_`) so Scizor never collides
 * with the host app's own preferences. A small in-memory cache backs the
 * synchronous getters used by feature flags and server selection; writes are
 * applied to the cache immediately and flushed to disk asynchronously.
 */
class ScizorStore internal constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = ConcurrentHashMap<String, Any>()

    /** Loads all persisted values into the cache. Safe to call from app start. */
    internal fun preload() {
        runCatching {
            runBlocking {
                val prefs = context.scizorDataStore.data.first()
                prefs.asMap().forEach { (key, value) ->
                    cache[key.name] = value
                }
            }
        }
    }

    fun boolean(key: String, default: Boolean): Boolean {
        return cache[prefixed(key)] as? Boolean ?: default
    }

    fun string(key: String, default: String? = null): String? {
        return cache[prefixed(key)] as? String ?: default
    }

    fun contains(key: String): Boolean = cache.containsKey(prefixed(key))

    /** A read-only snapshot of every persisted Scizor setting, for the Preferences browser. */
    internal fun snapshot(): Map<String, Any> = cache.toMap()

    fun putBoolean(key: String, value: Boolean) {
        val full = prefixed(key)
        cache[full] = value
        scope.launch {
            runCatching {
                context.scizorDataStore.edit { it[booleanPreferencesKey(full)] = value }
            }
        }
    }

    fun putString(key: String, value: String) {
        val full = prefixed(key)
        cache[full] = value
        scope.launch {
            runCatching {
                context.scizorDataStore.edit { it[stringPreferencesKey(full)] = value }
            }
        }
    }

    fun remove(key: String) {
        val full = prefixed(key)
        cache.remove(full)
        scope.launch {
            runCatching {
                context.scizorDataStore.edit { prefs ->
                    prefs.remove(booleanPreferencesKey(full))
                    prefs.remove(stringPreferencesKey(full))
                }
            }
        }
    }

    private fun prefixed(key: String): String =
        if (key.startsWith(PREFIX)) key else PREFIX + key

    companion object {
        const val PREFIX = "scizor_"
    }
}
