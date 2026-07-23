package com.scizor.feature.featureflags

import com.scizor.Scizor

/**
 * A feature flag registered by the host app.
 *
 * @param key stable identifier used in code (`FeatureFlags.isEnabled(key)`).
 * @param title human label shown in the menu.
 * @param defaultValue the "remote"/baseline value used when there is no override.
 */
data class FeatureFlag(
    val key: String,
    val title: String,
    val defaultValue: Boolean,
)

/** The three states a flag override can be in (mirrors Scyther's True/False/Remote). */
enum class FlagOverride { ON, OFF, REMOTE }

/**
 * Runtime-overridable feature flags with a tri-state override model. Each flag has
 * a baseline ([FeatureFlag.defaultValue], "Remote") and can be locally forced ON or
 * OFF, or left following the baseline (REMOTE). A global [overridesEnabled] switch
 * disables all local overrides at once. Everything persists via the Scizor store.
 */
object FeatureFlags {

    private val flags = LinkedHashMap<String, FeatureFlag>()

    fun register(flag: FeatureFlag) {
        flags[flag.key] = flag
    }

    fun all(): List<FeatureFlag> = flags.values.toList()

    /** Remote/baseline value for a flag (its registered default). */
    fun remoteValue(key: String): Boolean = flags[key]?.defaultValue ?: false

    /** Whether local overrides are honored at all. Off by default, matching Scyther. */
    var overridesEnabled: Boolean
        get() = Scizor.storeOrNull()?.boolean(OVERRIDES_ENABLED, false) ?: false
        set(value) {
            Scizor.storeOrNull()?.putBoolean(OVERRIDES_ENABLED, value)
        }

    fun isEnabled(key: String): Boolean {
        val flag = flags[key] ?: return false
        val store = Scizor.storeOrNull() ?: return flag.defaultValue
        if (!overridesEnabled) return flag.defaultValue
        return store.boolean(storeKey(key), flag.defaultValue)
    }

    fun overrideState(key: String): FlagOverride {
        val store = Scizor.storeOrNull() ?: return FlagOverride.REMOTE
        if (!store.contains(storeKey(key))) return FlagOverride.REMOTE
        return if (store.boolean(storeKey(key), false)) FlagOverride.ON else FlagOverride.OFF
    }

    fun setOverride(key: String, state: FlagOverride) {
        val store = Scizor.storeOrNull() ?: return
        when (state) {
            FlagOverride.ON -> store.putBoolean(storeKey(key), true)
            FlagOverride.OFF -> store.putBoolean(storeKey(key), false)
            FlagOverride.REMOTE -> store.remove(storeKey(key))
        }
    }

    fun isOverridden(key: String): Boolean =
        Scizor.storeOrNull()?.contains(storeKey(key)) == true

    /** Clears every local override, returning all flags to their remote value. */
    fun resetAllToRemote() {
        val store = Scizor.storeOrNull() ?: return
        flags.keys.forEach { store.remove(storeKey(it)) }
    }

    fun pinnedKeys(): List<String> =
        Scizor.storeOrNull()?.string(PINNED)?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()

    fun isPinned(key: String): Boolean = key in pinnedKeys()

    fun togglePin(key: String) {
        val current = pinnedKeys()
        val updated = if (key in current) current - key else current + key
        Scizor.storeOrNull()?.putString(PINNED, updated.joinToString(","))
    }

    private fun storeKey(key: String) = "flag_$key"

    private const val OVERRIDES_ENABLED = "flag_overrides_enabled"
    private const val PINNED = "flag_pinned"
}
