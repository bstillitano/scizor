package com.scizor

import android.content.pm.ApplicationInfo
import com.scizor.core.FeatureRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Proves the production gate on a facade that has never started.
 *
 * [Scizor] is an object, so its state survives from one test method to the next
 * and the first test to call `start()` wins for the whole run. That makes
 * "refusing does nothing" untestable in [ScizorFacadeTest], where some earlier
 * test has already initialised the store, installed the crash handler and
 * registered the built-in features.
 *
 * Robolectric caches one sandbox — and therefore one class loader, and one set
 * of statics — per SDK level. Pinning this class to an SDK no other test uses
 * (the rest run at 35, from `robolectric.properties`) hands it a [Scizor] that
 * nothing has touched, so every side effect `start()` would cause is still
 * observable by its absence.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScizorProductionGateTest {

    @Test
    fun `a refused start has no side effects, and the override lifts it`() {
        val app = RuntimeEnvironment.getApplication()
        val info = app.applicationInfo
        info.flags = info.flags and ApplicationInfo.FLAG_DEBUGGABLE.inv()

        val handlerBefore = Thread.getDefaultUncaughtExceptionHandler()

        Scizor.start(app)

        // Every side effect start() would otherwise cause, by its absence:
        assertNull("the DataStore-backed settings store was created", Scizor.storeOrNull())
        assertTrue("built-in features were registered", FeatureRegistry.all().isEmpty())
        assertSame(
            "the crash handler was installed",
            handlerBefore,
            Thread.getDefaultUncaughtExceptionHandler(),
        )
        // No shake detector either — it is installed by the same call, after the point
        // the gate returns from, and its sensor listener is not reachable from here.

        // Same non-debuggable build; the override is the only thing that changes.
        Scizor.start(app, allowProductionBuilds = true)

        assertNotNull("the override did not start Scizor", Scizor.storeOrNull())
        assertFalse("the override did not register the built-in features", FeatureRegistry.all().isEmpty())
    }
}
