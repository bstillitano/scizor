package com.scizor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

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
}
