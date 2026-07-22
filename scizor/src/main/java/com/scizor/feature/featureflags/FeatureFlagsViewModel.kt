package com.scizor.feature.featureflags

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class FlagUi(
    val key: String,
    val title: String,
    val enabled: Boolean,
    val overridden: Boolean,
)

internal class FeatureFlagsViewModel : ViewModel() {

    private val _flags = MutableStateFlow<List<FlagUi>>(emptyList())
    val flags: StateFlow<List<FlagUi>> = _flags.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _flags.value = FeatureFlags.all().map { flag ->
            FlagUi(
                key = flag.key,
                title = flag.title,
                enabled = FeatureFlags.isEnabled(flag.key),
                overridden = FeatureFlags.isOverridden(flag.key),
            )
        }
    }

    fun toggle(key: String, enabled: Boolean) {
        FeatureFlags.override(key, enabled)
        refresh()
    }

    fun reset(key: String) {
        FeatureFlags.override(key, null)
        refresh()
    }
}
