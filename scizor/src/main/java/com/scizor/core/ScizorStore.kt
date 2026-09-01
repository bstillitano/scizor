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
 * The move is deliberately timid, because its only failure mode is data loss.
 * The legacy file is deleted only after its contents have been read *and*
 * written to the destination: a read that fails is retried on the next launch
 * rather than treated as "there was nothing there". And when the two locations
 * resolve to the same file — which is what happens when
 * `createDeviceProtectedStorageContext()` is unavailable and [deviceContext]
 * falls back to the ordinary one — the migration is skipped outright, since
 * "migrating" a file onto itself can only ever destroy it.
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

    private val storeFile = File(deviceContext.filesDir, DATASTORE_PATH)

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { storeFile },
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
     * location cannot cause a second run. The legacy file is removed only once
     * its contents are safely in the destination — see [LegacyRead].
     */
    private suspend fun migrateIfNeeded() {
        val migrated = booleanPreferencesKey(MIGRATION_KEY)
        if (dataStore.data.first()[migrated] == true) return

        // Source and destination can be the same file: `deviceContext` falls back to
        // the ordinary context when device-protected storage is unavailable, and then
        // both paths are `filesDir/DATASTORE_PATH`. Reading that as a "legacy" file
        // opens a second DataStore on a file that already has one, which DataStore
        // refuses — and the old code read that failure as "no legacy data" and deleted
        // the live settings. Compare resolved paths rather than the contexts, so the
        // guard covers every way the two can coincide, not just the known one.
        if (isSameFile(legacyFile, storeFile)) {
            dataStore.edit { it[migrated] = true }
            return
        }

        when (val legacy = readLegacy()) {
            LegacyRead.Absent -> dataStore.edit { it[migrated] = true }

            // The file is there but could not be read. Keep it, and leave the flag
            // unset so the next launch tries again; deleting now would throw away
            // data that a later read may well recover.
            LegacyRead.Unreadable -> Unit

            is LegacyRead.Loaded -> {
                dataStore.edit { destination ->
                    migratePreferences(legacy.preferences, destination)
                    destination[migrated] = true
                }
                legacyFile.delete()
            }
        }
    }

    /**
     * Outcome of reading the pre-migration DataStore file.
     *
     * [Absent] and [Unreadable] have to be told apart: only the first means the
     * legacy file can be discarded.
     */
    private sealed interface LegacyRead {

        /** No legacy file exists — the migration has nothing to do. */
        data object Absent : LegacyRead

        /** A legacy file exists but could not be read. Its data may still be recoverable. */
        data object Unreadable : LegacyRead

        /** The legacy file was read in full. */
        data class Loaded(val preferences: Preferences) : LegacyRead
    }

    /** Reads the pre-migration DataStore file. */
    private suspend fun readLegacy(): LegacyRead {
        if (!legacyFile.exists()) return LegacyRead.Absent
        // A private job, torn down as soon as the read finishes, so DataStore stops
        // treating the legacy file as having a live instance in this process.
        val legacyJob = SupervisorJob()
        val legacyScope = CoroutineScope(legacyJob + Dispatchers.IO)
        return try {
            runCatching {
                val legacy = PreferenceDataStoreFactory.create(
                    scope = legacyScope,
                    produceFile = { legacyFile },
                )
                LegacyRead.Loaded(legacy.data.first())
            }.getOrDefault(LegacyRead.Unreadable)
        } finally {
            legacyJob.cancel()
            legacyJob.join()
        }
    }

    /**
     * True when [a] and [b] name the same file on disk. Falls back to comparing
     * absolute paths if either canonical path cannot be resolved.
     */
    private fun isSameFile(a: File, b: File): Boolean = runCatching {
        a.canonicalPath == b.canonicalPath
    }.getOrDefault(a.absolutePath == b.absolutePath)

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
        internal const val DATASTORE_PATH = "datastore/scizor_settings.preferences_pb"

        /** Records that the one-time move out of credential-encrypted storage has run. */
        private const val MIGRATION_KEY = "scizor_defaults_did_migrate"
    }
}
