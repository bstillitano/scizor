package com.scizor.feature.servers

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class ServersUiState(
    val environments: List<ServerEnvironment> = emptyList(),
    val selectedName: String? = null,
)

internal class ServersViewModel : ViewModel() {

    private val _state = MutableStateFlow(ServersUiState())
    val state: StateFlow<ServersUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = ServersUiState(
            environments = ServerConfiguration.all(),
            selectedName = ServerConfiguration.selected?.name,
        )
    }

    fun select(environment: ServerEnvironment) {
        ServerConfiguration.select(environment)
        refresh()
    }
}
