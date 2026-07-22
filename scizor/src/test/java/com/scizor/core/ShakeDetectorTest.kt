package com.scizor.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeDetectorTest {

    @Test
    fun `still device at rest is not a shake`() {
        // ~1g downward (gravity only)
        assertFalse(ShakeDetector.isShake(0f, 9.81f, 0f))
    }

    @Test
    fun `hard jolt exceeds the shake threshold`() {
        // ~4g on one axis
        assertTrue(ShakeDetector.isShake(0f, 39.2f, 0f))
    }

    @Test
    fun `moderate movement below threshold is not a shake`() {
        // ~2g combined, under the 2.7g threshold
        assertFalse(ShakeDetector.isShake(9.81f, 9.81f, 9.81f))
    }
}
