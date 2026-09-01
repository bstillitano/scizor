package com.scizor.core

import android.content.Context
import android.content.ContextWrapper
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

class ScizorStoreTest {

    private val legacyOnly = stringPreferencesKey("scizor_selected_server")
    private val inBoth = booleanPreferencesKey("scizor_flag_overrides_enabled")
    private val destinationOnly = stringPreferencesKey("scizor_flag_pinned")

    @Test
    fun `legacy-only key is copied into the destination`() {
        val source = mutablePreferencesOf(legacyOnly to "staging")
        val destination = mutablePreferencesOf()

        migratePreferences(source, destination)

        assertEquals("staging", destination[legacyOnly])
    }

    @Test
    fun `destination value wins over a stale legacy value`() {
        val source = mutablePreferencesOf(inBoth to false)
        val destination = mutablePreferencesOf(inBoth to true)

        migratePreferences(source, destination)

        assertEquals(true, destination[inBoth])
    }

    @Test
    fun `keys already only in the destination are left alone`() {
        val source = mutablePreferencesOf(legacyOnly to "staging")
        val destination = mutablePreferencesOf(destinationOnly to "a,b")

        migratePreferences(source, destination)

        assertEquals("a,b", destination[destinationOnly])
        assertEquals("staging", destination[legacyOnly])
    }

    @Test
    fun `an empty source leaves the destination untouched`() {
        val source = mutablePreferencesOf()
        val destination = mutablePreferencesOf(destinationOnly to "a,b")

        migratePreferences(source, destination)

        assertEquals(1, destination.asMap().size)
        assertEquals("a,b", destination[destinationOnly])
    }

    @Test
    fun `migration does not invent the completion flag itself`() {
        val source = mutablePreferencesOf(legacyOnly to "staging")
        val destination = mutablePreferencesOf()

        migratePreferences(source, destination)

        // The flag is written by migrateIfNeeded, not by the pure copy — keeping
        // the copy free of policy is what makes it testable in isolation.
        assertNull(destination[booleanPreferencesKey("scizor_defaults_did_migrate")])
    }
}

/**
 * Migration tests for [ScizorStore.preload].
 *
 * The pure copy is covered above; these cover the policy around it — the run-once
 * guard, where the completion flag lives, when the legacy file may be deleted, and
 * the case where source and destination turn out to be the same file.
 *
 * The two storage locations come from [MigrationContext] rather than Robolectric's
 * own device-protected support, so each case is set up explicitly. The fallback
 * that makes both locations coincide is reproduced by returning `this` from
 * `createDeviceProtectedStorageContext()` — exactly what [ScizorStore] ends up
 * with when the real call is unavailable.
 */
@RunWith(RobolectricTestRunner::class)
class ScizorStoreMigrationTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val serverKey = stringPreferencesKey("scizor_selected_server")
    private val migratedKey = booleanPreferencesKey("scizor_defaults_did_migrate")

    /**
     * A context with an explicit `filesDir`, whose device-protected variant is
     * [deviceProtected] — or itself, reproducing [ScizorStore]'s fallback.
     */
    private class MigrationContext(
        base: Context,
        private val files: File,
        private val deviceProtected: Context? = null,
    ) : ContextWrapper(base) {
        override fun getFilesDir(): File = files.apply { mkdirs() }
        override fun createDeviceProtectedStorageContext(): Context = deviceProtected ?: this
    }

    private fun storeFile(filesDir: File) = File(filesDir, ScizorStore.DATASTORE_PATH)

    /** A context whose credential-encrypted and device-protected locations differ. */
    private fun splitContext(credential: File, device: File): Context {
        val app = RuntimeEnvironment.getApplication()
        return MigrationContext(app, credential, MigrationContext(app, device))
    }

    /**
     * Writes [edits] to a DataStore at [file], then fully tears it down — DataStore
     * only releases its claim on a file when the owning job has finished, so the
     * seeding instance has to be joined, not merely cancelled, before the code under
     * test opens the same path.
     */
    private fun seed(file: File, edits: MutablePreferences.() -> Unit) {
        file.parentFile?.mkdirs()
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.IO)
        runBlocking {
            try {
                PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
                    .edit { it.edits() }
            } finally {
                job.cancel()
                job.join()
            }
        }
    }

    @Test
    fun `values from a legacy file are readable after preload`() {
        val credential = temp.newFolder("credential")
        val device = temp.newFolder("device")
        seed(storeFile(credential)) { set(serverKey, "staging") }

        val store = ScizorStore(splitContext(credential, device))
        store.preload()

        assertEquals("staging", store.string("selected_server"))
    }

    @Test
    fun `the legacy file is deleted once its contents have been copied`() {
        val credential = temp.newFolder("credential")
        val device = temp.newFolder("device")
        val legacy = storeFile(credential)
        seed(legacy) { set(serverKey, "staging") }

        val store = ScizorStore(splitContext(credential, device))
        store.preload()

        assertFalse("legacy file should be gone after a successful copy", legacy.exists())
        assertTrue("destination should have been written", storeFile(device).exists())
    }

    @Test
    fun `a second preload does not migrate again`() {
        val credential = temp.newFolder("credential")
        val device = temp.newFolder("device")
        val legacy = storeFile(credential)
        seed(legacy) { set(serverKey, "staging") }

        val store = ScizorStore(splitContext(credential, device))
        store.preload()

        // A legacy file reappearing must not be picked up: the flag lives in the
        // destination, so recreating the source cannot re-trigger the move.
        seed(legacy) { set(serverKey, "production") }
        store.preload()

        assertEquals("staging", store.string("selected_server"))
        assertTrue("a re-created legacy file must be left alone", legacy.exists())
    }

    @Test
    fun `an unreadable legacy file is kept rather than deleted`() {
        val credential = temp.newFolder("credential")
        val device = temp.newFolder("device")
        val legacy = storeFile(credential)
        legacy.parentFile?.mkdirs()
        legacy.writeBytes(byteArrayOf(0x1, 0x2, 0x3, 0x4, 0x5))

        val store = ScizorStore(splitContext(credential, device))
        store.preload()

        // A read failure is not evidence that there was nothing to migrate, so the
        // file stays put and the migration is retried on the next launch.
        assertTrue("an unreadable legacy file must survive", legacy.exists())
        assertEquals(5L, legacy.length())
    }

    @Test
    fun `settings survive when both locations resolve to the same file`() {
        // Reproduces the device-protected fallback: `deviceContext === context`, so the
        // legacy path and the live store path are one and the same file. Migrating a
        // file onto itself has to be skipped, not attempted and then "cleaned up".
        val files = temp.newFolder("shared")
        val file = storeFile(files)
        seed(file) { set(serverKey, "staging") }
        val seededLength = file.length()

        val store = ScizorStore(MigrationContext(RuntimeEnvironment.getApplication(), files))
        store.preload()

        assertTrue("the live settings file must not be deleted", file.exists())
        assertTrue("the live settings file must not be emptied", file.length() >= seededLength)
        assertEquals("staging", store.string("selected_server"))
    }

    @Test
    fun `the completion flag is written to the destination`() {
        val credential = temp.newFolder("credential")
        val device = temp.newFolder("device")
        seed(storeFile(credential)) { set(serverKey, "staging") }

        val store = ScizorStore(splitContext(credential, device))
        store.preload()

        assertEquals(true, store.snapshot()[migratedKey.name])
    }
}
