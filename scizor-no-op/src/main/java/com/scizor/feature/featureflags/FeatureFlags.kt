package com.scizor.feature.featureflags

/** No-op mirror of the real [FeatureFlag]. */
data class FeatureFlag(
    val key: String,
    val title: String,
    val defaultValue: Boolean,
)

/**
 * No-op mirror of the real [FeatureFlags]. Overrides are impossible in release,
 * so [isEnabled] returns each flag's registered default — the app behaves as if
 * no debug overrides exist.
 */
object FeatureFlags {

    private val flags = LinkedHashMap<String, FeatureFlag>()

    fun register(flag: FeatureFlag) {
        flags[flag.key] = flag
    }

    fun all(): List<FeatureFlag> = flags.values.toList()

    fun isEnabled(key: String): Boolean = flags[key]?.defaultValue ?: false

    fun isOverridden(key: String): Boolean = false

    @Suppress("UNUSED_PARAMETER")
    fun override(key: String, value: Boolean?) = Unit
}
