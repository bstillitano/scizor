package com.scizor.feature.featureflags

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class FlagUi(
    val key: String,
    val title: String,
    val remoteValue: Boolean,
    val state: FlagOverride,
    val pinned: Boolean,
)

internal data class FeatureFlagsUiState(
    val overridesEnabled: Boolean = true,
    val flags: List<FlagUi> = emptyList(),
)

internal class FeatureFlagsViewModel : ViewModel() {

    private val _state = MutableStateFlow(FeatureFlagsUiState())
    val state: StateFlow<FeatureFlagsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = FeatureFlagsUiState(
            overridesEnabled = FeatureFlags.overridesEnabled,
            flags = FeatureFlags.all()
                .sortedBy { it.title.lowercase() }
                .map { flag ->
                    FlagUi(
                        key = flag.key,
                        title = flag.title,
                        remoteValue = flag.defaultValue,
                        state = FeatureFlags.overrideState(flag.key),
                        pinned = FeatureFlags.isPinned(flag.key),
                    )
                },
        )
    }

    fun setOverridesEnabled(enabled: Boolean) {
        FeatureFlags.overridesEnabled = enabled
        refresh()
    }

    fun setState(key: String, state: FlagOverride) {
        FeatureFlags.setOverride(key, state)
        refresh()
    }

    fun togglePin(key: String) {
        FeatureFlags.togglePin(key)
        refresh()
    }

    fun resetAll() {
        FeatureFlags.resetAllToRemote()
        refresh()
    }
}
