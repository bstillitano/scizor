package com.scizor.feature.crashlogs

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CrashLoggerTest {

    @Test
    fun `records and reads back a crash, then clears`() {
        val context = RuntimeEnvironment.getApplication()
        CrashLogger.clear(context)

        CrashLogger.recordForDemo(context, RuntimeException("boom"))

        val crashes = CrashLogger.crashes(context)
        assertTrue(
            crashes.any { it.type == "java.lang.RuntimeException" && it.message == "boom" },
        )
        assertTrue(crashes.first().stackTrace.contains("RuntimeException"))

        CrashLogger.clear(context)
        assertTrue(CrashLogger.crashes(context).isEmpty())
    }
}
