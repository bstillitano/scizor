package com.scizor.feature.preferences

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PreferencesBrowserTest {

    private val file = "browser_test_prefs"

    private fun seed(context: Context) {
        context.getSharedPreferences(file, Context.MODE_PRIVATE)
            .edit()
            .putString("name", "brandon")
            .putBoolean("dark_mode", true)
            .apply()
    }

    @Test
    fun `entries reflects stored values`() {
        val context = RuntimeEnvironment.getApplication()
        seed(context)

        val entries = PreferencesBrowser.entries(context, file)
        val name = entries.first { it.key == "name" }
        assertEquals("brandon", name.value)
        assertEquals("String", name.type)
        assertTrue(entries.any { it.key == "dark_mode" && it.value == "true" })
    }

    @Test
    fun `putString updates a value`() {
        val context = RuntimeEnvironment.getApplication()
        seed(context)

        PreferencesBrowser.putString(context, file, "name", "changed")

        val name = PreferencesBrowser.entries(context, file).first { it.key == "name" }
        assertEquals("changed", name.value)
    }

    @Test
    fun `remove deletes an entry`() {
        val context = RuntimeEnvironment.getApplication()
        seed(context)

        PreferencesBrowser.remove(context, file, "name")

        val name = PreferencesBrowser.entries(context, file).firstOrNull { it.key == "name" }
        assertNull(name)
    }
}
