package com.scizor.feature.console

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal data class ConsoleFilter(
    val level: LogLevel? = null,
    val query: String = "",
)

internal class ConsoleViewModel : ViewModel() {

    private val _filter = MutableStateFlow(ConsoleFilter())
    val filter: StateFlow<ConsoleFilter> = _filter

    val entries: StateFlow<List<LogEntry>> =
        combine(ConsoleLogger.entries, _filter) { entries, filter ->
            entries.filter { entry ->
                (filter.level == null || entry.level == filter.level) &&
                    (filter.query.isBlank() ||
                        entry.message.contains(filter.query, ignoreCase = true) ||
                        entry.tag.contains(filter.query, ignoreCase = true))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setLevel(level: LogLevel?) {
        _filter.value = _filter.value.copy(level = level)
    }

    fun setQuery(query: String) {
        _filter.value = _filter.value.copy(query = query)
    }

    fun clear() = ConsoleLogger.clear()
}
