package com.scizor.feature.console

/** Severity of a captured log line. */
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

/** A single parsed Logcat line. */
data class LogEntry(
    val time: String,
    val level: LogLevel,
    val tag: String,
    val message: String,
)
