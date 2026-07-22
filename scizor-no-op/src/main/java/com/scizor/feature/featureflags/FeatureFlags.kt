package com.scizor.feature.featureflags

/** No-op mirror of the real [FeatureFlag]. */
data class FeatureFlag(
    val key: String,
    val title: String,
    val defaultValue: Boolean,
)

/** No-op mirror of the real [FlagOverride]. */
enum class FlagOverride { ON, OFF, REMOTE }

/**
 * No-op mirror of the real [FeatureFlags]. Overrides are impossible in release,
 * so flags always resolve to their registered remote/default value.
 */
object FeatureFlags {

    private val flags = LinkedHashMap<String, FeatureFlag>()

    fun register(flag: FeatureFlag) {
        flags[flag.key] = flag
    }

    fun all(): List<FeatureFlag> = flags.values.toList()

    fun remoteValue(key: String): Boolean = flags[key]?.defaultValue ?: false

    var overridesEnabled: Boolean = true

    fun isEnabled(key: String): Boolean = flags[key]?.defaultValue ?: false

    fun overrideState(key: String): FlagOverride = FlagOverride.REMOTE

    @Suppress("UNUSED_PARAMETER")
    fun setOverride(key: String, state: FlagOverride) = Unit

    fun isOverridden(key: String): Boolean = false

    fun resetAllToRemote() = Unit
}
