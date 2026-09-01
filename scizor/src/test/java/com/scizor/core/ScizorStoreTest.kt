package com.scizor.core

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
