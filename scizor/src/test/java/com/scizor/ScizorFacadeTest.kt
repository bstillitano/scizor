package com.scizor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        // Reference cleared in onDestroy; nothing to finish, and no throw.
        Scizor.dismiss()
    }
}
