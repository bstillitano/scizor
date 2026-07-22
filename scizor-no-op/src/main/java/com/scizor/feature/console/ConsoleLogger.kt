package com.scizor.feature.console

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** No-op mirror of the real [LogLevel]. */
enum class LogLevel(val letter: Char) {
    VERBOSE('V'),
    DEBUG('D'),
    INFO('I'),
    WARN('W'),
    ERROR('E'),
    ASSERT('A');

    companion object {
        fun from(letter: Char): LogLevel =
            entries.firstOrNull { it.letter == letter } ?: DEBUG
    }
}

/** No-op mirror of the real [LogEntry]. */
data class LogEntry(
    val time: String,
    val level: LogLevel,
    val tag: String,
    val message: String,
)

/** No-op mirror of the real [ConsoleLogger]. */
object ConsoleLogger {
    val entries: StateFlow<List<LogEntry>> = MutableStateFlow(emptyList())
    fun start() = Unit
    fun stop() = Unit
    fun clear() = Unit
}
