package com.scizor.feature.featureflags

import com.scizor.Scizor
import org.junit.Assert.assertEquals
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
        FeatureFlags.overridesEnabled = true
    }

    @Test
    fun `isEnabled returns remote value with no override`() {
        FeatureFlags.register(FeatureFlag("ff_default_on", "Default on", defaultValue = true))
        assertTrue(FeatureFlags.isEnabled("ff_default_on"))
        assertEquals(FlagOverride.REMOTE, FeatureFlags.overrideState("ff_default_on"))
    }

    @Test
    fun `override forces the flag value`() {
        FeatureFlags.register(FeatureFlag("ff_override", "Override", defaultValue = true))
        FeatureFlags.setOverride("ff_override", FlagOverride.OFF)
        assertFalse(FeatureFlags.isEnabled("ff_override"))
        assertEquals(FlagOverride.OFF, FeatureFlags.overrideState("ff_override"))
        assertTrue(FeatureFlags.isOverridden("ff_override"))
    }

    @Test
    fun `remote state reverts to default`() {
        FeatureFlags.register(FeatureFlag("ff_revert", "Revert", defaultValue = true))
        FeatureFlags.setOverride("ff_revert", FlagOverride.OFF)
        FeatureFlags.setOverride("ff_revert", FlagOverride.REMOTE)
        assertTrue(FeatureFlags.isEnabled("ff_revert"))
        assertEquals(FlagOverride.REMOTE, FeatureFlags.overrideState("ff_revert"))
    }

    @Test
    fun `disabling overrides ignores local values`() {
        FeatureFlags.register(FeatureFlag("ff_global", "Global", defaultValue = true))
        FeatureFlags.setOverride("ff_global", FlagOverride.OFF)
        FeatureFlags.overridesEnabled = false
        // With overrides globally off, the flag follows its remote value.
        assertTrue(FeatureFlags.isEnabled("ff_global"))
        FeatureFlags.overridesEnabled = true
        assertFalse(FeatureFlags.isEnabled("ff_global"))
    }

    @Test
    fun `resetAllToRemote clears overrides`() {
        FeatureFlags.register(FeatureFlag("ff_reset", "Reset", defaultValue = true))
        FeatureFlags.setOverride("ff_reset", FlagOverride.OFF)
        FeatureFlags.resetAllToRemote()
        assertEquals(FlagOverride.REMOTE, FeatureFlags.overrideState("ff_reset"))
    }
}
