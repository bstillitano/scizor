package com.scizor.feature.featureflags

import com.scizor.Scizor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FeatureFlagsTest {

    @Before
    fun setUp() {
        Scizor.start(RuntimeEnvironment.getApplication())
    }

    @Test
    fun `isEnabled returns registered default`() {
        FeatureFlags.register(FeatureFlag("ff_default_on", "Default on", defaultValue = true))
        assertTrue(FeatureFlags.isEnabled("ff_default_on"))
    }

    @Test
    fun `override wins over default`() {
        FeatureFlags.register(FeatureFlag("ff_override", "Override", defaultValue = true))
        FeatureFlags.override("ff_override", false)
        assertFalse(FeatureFlags.isEnabled("ff_override"))
        assertTrue(FeatureFlags.isOverridden("ff_override"))
    }

    @Test
    fun `null override reverts to default`() {
        FeatureFlags.register(FeatureFlag("ff_revert", "Revert", defaultValue = true))
        FeatureFlags.override("ff_revert", false)
        FeatureFlags.override("ff_revert", null)
        assertTrue(FeatureFlags.isEnabled("ff_revert"))
        assertFalse(FeatureFlags.isOverridden("ff_revert"))
    }

    @Test
    fun `unknown flag is disabled`() {
        assertFalse(FeatureFlags.isEnabled("does_not_exist"))
    }
}
