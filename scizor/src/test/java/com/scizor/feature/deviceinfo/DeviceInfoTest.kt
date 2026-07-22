package com.scizor.feature.deviceinfo

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DeviceInfoTest {

    @Test
    fun `collect returns core device and app rows`() {
        val context = RuntimeEnvironment.getApplication()
        val rows = DeviceInfo.collect(context)

        val labels = rows.map { it.label }
        assertTrue(labels.contains("Model"))
        assertTrue(labels.contains("API Level"))
        assertTrue(labels.contains("Version"))
        assertTrue(labels.contains("Package"))
        assertTrue(labels.contains("Type"))
    }

    @Test
    fun `api level row matches build sdk int`() {
        val context = RuntimeEnvironment.getApplication()
        val rows = DeviceInfo.collect(context)

        val apiLevel = rows.first { it.label == "API Level" }
        assertEquals(Build.VERSION.SDK_INT.toString(), apiLevel.value)
    }

    @Test
    fun `package row reflects context package name`() {
        val context = RuntimeEnvironment.getApplication()
        val rows = DeviceInfo.collect(context)

        val pkg = rows.firstOrNull { it.label == "Package" }
        assertNotNull(pkg)
        assertEquals(context.packageName, pkg!!.value)
    }
}
