package com.scizor.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Copies every entry of [source] into [destination] that [destination] does not
 * already hold.
 *
 * Newer state is never clobbered by a stale value. Kept free of any policy — the
 * run-once guard and the completion flag live in [ScizorStore.migrateIfNeeded] —
 * so this stays directly testable without a `Context`.
 */
@Suppress("UNCHECKED_CAST")
internal fun migratePreferences(source: Preferences, destination: MutablePreferences) {
    source.asMap().forEach { (key, value) ->
        val typed = key as Preferences.Key<Any>
        if (destination[typed] == null) {
            destination[typed] = value
        }
    }
}

/**
 * Thin persistence layer over Jetpack DataStore.
 *
 * ## Why device-protected storage
 *
 * Every setting Scizor persists is written to device-protected storage
 * (`/data/user_de/0/<pkg>/files`) rather than the credential-encrypted `filesDir`
 * (`/data/user/0/<pkg>/files`) the rest of the app uses. Apps commonly wipe their
 * own storage when a user signs out:
 *
 * ```
 * context.filesDir.deleteRecursively()
 * context.getSharedPreferences(name, MODE_PRIVATE).edit().clear().apply()
 * ```
 *
 * Neither reaches device-protected storage, so feature flag overrides, the
 * selected server, pins and spoofed locations survive a sign-out. This mirrors
 * Scyther's use of a named `UserDefaults` suite, which is likewise a separate
 * persistent domain from the one a host app clears.
 *
 * A full container wipe — `ActivityManager.clearApplicationUserData()`, or the
 * user's own "Clear storage" — still removes it. That is not an accidental clear,
 * and Scyther has the same limit.
 *
 * ## Migration
 *
 * Earlier versions wrote to the credential-encrypted `filesDir`. The first
 * [preload] after upgrading moves every key across via [migratePreferences] and
 * deletes the legacy file. The completion flag is stored in the destination, so
 * clearing the source can never cause the migration to run a second time.
 *
 * All keys are namespaced with [PREFIX] (`scizor_`) so Scizor never collides with
 * the host app's own preferences. A small in-memory cache backs the synchronous
 * getters used by feature flags and server selection; writes are applied to the
 * cache immediately and flushed to disk asynchronously.
 */
class ScizorStore internal constructor(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = ConcurrentHashMap<String, Any>()

    /** Falls back to the credential-encrypted context if the device-protected one is unavailable. */
    private val deviceContext: Context =
        runCatching { context.createDeviceProtectedStorageContext() }.getOrNull() ?: context

    private val legacyFile = File(context.filesDir, DATASTORE_PATH)

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { File(deviceContext.filesDir, DATASTORE_PATH) },
    )

    /** Loads all persisted values into the cache. Safe to call from app start. */
    internal fun preload() {
        runCatching {
            runBlocking {
                migrateIfNeeded()
                val prefs = dataStore.data.first()
                prefs.asMap().forEach { (key, value) ->
                    cache[key.name] = value
                }
            }
        }
    }

    /**
     * Runs [migratePreferences] once, then records that it has happened.
     *
     * The flag is written into the destination so that clearing the legacy
     * location cannot cause a second run.
     */
    private suspend fun migrateIfNeeded() {
        val migrated = booleanPreferencesKey(MIGRATION_KEY)
        if (dataStore.data.first()[migrated] == true) return

        val legacy = readLegacy()
        dataStore.edit { destination ->
            if (legacy != null) migratePreferences(legacy, destination)
            destination[migrated] = true
        }
        legacyFile.delete()
    }

    /** Reads the pre-migration DataStore file, or null if there isn't one. */
    private suspend fun readLegacy(): Preferences? {
        if (!legacyFile.exists()) return null
        return runCatching {
            PreferenceDataStoreFactory.create(produceFile = { legacyFile }).data.first()
        }.getOrNull()
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
                dataStore.edit { it[booleanPreferencesKey(full)] = value }
            }
        }
    }

    fun putString(key: String, value: String) {
        val full = prefixed(key)
        cache[full] = value
        scope.launch {
            runCatching {
                dataStore.edit { it[stringPreferencesKey(full)] = value }
            }
        }
    }

    fun remove(key: String) {
        val full = prefixed(key)
        cache.remove(full)
        scope.launch {
            runCatching {
                dataStore.edit { prefs ->
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

        /** Path of the DataStore file, relative to whichever `filesDir` hosts it. */
        private const val DATASTORE_PATH = "datastore/scizor_settings.preferences_pb"

        /** Records that the one-time move out of credential-encrypted storage has run. */
        private const val MIGRATION_KEY = "scizor_defaults_did_migrate"
    }
}
