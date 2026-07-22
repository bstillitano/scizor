package com.scizor

import org.junit.Assert.assertEquals
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
}
