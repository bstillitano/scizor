package com.scizor.feature.featureflags

import com.scizor.Scizor

/**
 * A boolean feature flag registered by the host app.
 *
 * @param key stable identifier used in code (`FeatureFlags.isEnabled(key)`).
 * @param title human label shown in the menu.
 * @param defaultValue value used when there is no runtime override.
 */
data class FeatureFlag(
    val key: String,
    val title: String,
    val defaultValue: Boolean,
)

/**
 * Runtime-overridable feature flags. Register defaults at startup, read with
 * [isEnabled], and override from the debug menu. Overrides persist via the
 * Scizor store under `scizor_flag_<key>`.
 */
object FeatureFlags {

    private val flags = LinkedHashMap<String, FeatureFlag>()

    fun register(flag: FeatureFlag) {
        flags[flag.key] = flag
    }

    fun all(): List<FeatureFlag> = flags.values.toList()

    fun isEnabled(key: String): Boolean {
        val flag = flags[key] ?: return false
        val store = Scizor.storeOrNull() ?: return flag.defaultValue
        return store.boolean(storeKey(key), flag.defaultValue)
    }

    fun isOverridden(key: String): Boolean =
        Scizor.storeOrNull()?.contains(storeKey(key)) == true

    /** Sets an override, or reverts to the registered default when [value] is null. */
    fun override(key: String, value: Boolean?) {
        val store = Scizor.storeOrNull() ?: return
        if (value == null) {
            store.remove(storeKey(key))
        } else {
            store.putBoolean(storeKey(key), value)
        }
    }

    private fun storeKey(key: String) = "flag_$key"
}
