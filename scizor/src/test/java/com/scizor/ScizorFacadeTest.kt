package com.scizor

import android.content.pm.ApplicationInfo
import android.util.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
class ScizorFacadeTest {

    @Test
    fun `start sets defaults and config round-trips`() {
        val app = RuntimeEnvironment.getApplication()
        Scizor.start(app)

        assertEquals(ScizorGesture.SHAKE, Scizor.invocationGesture)

        Scizor.environmentVariables = mapOf("API" to "https://example.com")
        assertEquals("https://example.com", Scizor.environmentVariables["API"])
    }

    @Test
    fun `changing invocation gesture is retained`() {
        val app = RuntimeEnvironment.getApplication()
        Scizor.start(app)

        Scizor.invocationGesture = ScizorGesture.NONE
        assertEquals(ScizorGesture.NONE, Scizor.invocationGesture)

        // reset for other tests / callers
        Scizor.invocationGesture = ScizorGesture.SHAKE
        assertTrue(Scizor.invocationGesture == ScizorGesture.SHAKE)
    }

    @Test
    fun `dismiss is a no-op when the menu is not open`() {
        val app = RuntimeEnvironment.getApplication()
        Scizor.start(app)

        // Reset so this test's outcome does not depend on JUnit's execution order.
        Scizor.activityRef = null

        // No activity is live; this must not throw.
        Scizor.dismiss()
    }

    @Test
    fun `dismiss finishes a live menu activity`() {
        val app = RuntimeEnvironment.getApplication()
        Scizor.start(app)

        val controller = org.robolectric.Robolectric
            .buildActivity(com.scizor.core.ScizorActivity::class.java)
            .setup()

        assertFalse(controller.get().isFinishing)
        Scizor.dismiss()
        assertTrue(controller.get().isFinishing)
    }

    @Test
    fun `dismiss does not retain the activity after it is destroyed`() {
        val app = RuntimeEnvironment.getApplication()
        Scizor.start(app)

        org.robolectric.Robolectric
            .buildActivity(com.scizor.core.ScizorActivity::class.java)
            .setup()
            .destroy()

        // Reference cleared in onDestroy — not merely "not thrown", but actually gone.
        assertNull(Scizor.activityRef?.get())

        // And dismiss() on the now-empty reference is a safe no-op.
        Scizor.dismiss()
    }

    @Test
    fun `host-assigned properties are snapshot state`() {
        val app = RuntimeEnvironment.getApplication()
        Scizor.start(app)

        // Applying a snapshot reports every state object written in it. A property
        // backed by mutableStateOf contributes one; a plain var contributes nothing.
        //
        // Note the observer reports the backing state object, not Scizor itself —
        // do not try to identify the changed object by `it === Scizor`.
        var changedObjects = 0
        val observer = androidx.compose.runtime.snapshots.Snapshot.registerApplyObserver { changed, _ ->
            changedObjects += changed.size
        }

        try {
            androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                Scizor.fcmToken = "token-a"
            }
            assertTrue("expected the write to notify a snapshot observer", changedObjects > 0)
        } finally {
            observer.dispose()
            Scizor.fcmToken = null
        }
    }

    @Test
    fun `disabled features are hidden from the menu`() {
        val app = RuntimeEnvironment.getApplication()
        Scizor.start(app)

        try {
            Scizor.disabledFeatures = setOf("keystore", "console")

            val titles = com.scizor.ui.MenuViewModel().groups(app, null, emptyList())
                .flatMap { it.rows }
                .filterIsInstance<com.scizor.ui.MenuRow.Action>()
                .map { it.title }

            assertFalse(titles.contains("Keystore Browser"))
            assertFalse(titles.contains("Console Logger"))
            assertTrue(titles.contains("Feature Flags"))
        } finally {
            Scizor.disabledFeatures = emptySet()
        }
    }

    @Test
    fun `start runs under a debuggable application`() {
        val app = RuntimeEnvironment.getApplication()
        // Robolectric's application carries FLAG_DEBUGGABLE, which is what lets every
        // other test in this class call start() at all.
        assertTrue((app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0)

        ShadowLog.clear()
        Scizor.start(app)

        assertTrue(gateRefusals().isEmpty())
        assertNotNull(Scizor.storeOrNull())
    }

    @Test
    fun `start refuses when the build is not debuggable`() {
        val app = RuntimeEnvironment.getApplication()
        withDebuggable(false) {
            val storeBefore = Scizor.storeOrNull()
            ShadowLog.clear()

            Scizor.start(app)

            // The refusal is loud: a developer who wires Scizor into a release build
            // and sees nothing can find out why from Logcat.
            assertEquals(1, gateRefusals().size)
            // And it returns before touching anything — the store is neither created
            // nor rebuilt. ScizorProductionGateTest proves the same on a facade that
            // has never started, where every side effect is still observable.
            assertSame(storeBefore, Scizor.storeOrNull())
        }
    }

    @Test
    fun `allowProductionBuilds overrides the gate`() {
        val app = RuntimeEnvironment.getApplication()
        withDebuggable(false) {
            ShadowLog.clear()

            Scizor.start(app, allowProductionBuilds = true)

            assertTrue(gateRefusals().isEmpty())
            assertNotNull(Scizor.storeOrNull())
        }
    }

    /** Warnings written by the production gate, and nothing else. */
    private fun gateRefusals(): List<ShadowLog.LogItem> =
        ShadowLog.getLogs().filter {
            it.type == Log.WARN && it.tag == "Scizor" && it.msg.contains("not debuggable")
        }

    /** Runs [body] with the Robolectric application's debuggable flag forced to [debuggable]. */
    private fun withDebuggable(debuggable: Boolean, body: () -> Unit) {
        val info = RuntimeEnvironment.getApplication().applicationInfo
        val original = info.flags
        info.flags = if (debuggable) {
            original or ApplicationInfo.FLAG_DEBUGGABLE
        } else {
            original and ApplicationInfo.FLAG_DEBUGGABLE.inv()
        }
        try {
            body()
        } finally {
            info.flags = original
        }
    }
}
