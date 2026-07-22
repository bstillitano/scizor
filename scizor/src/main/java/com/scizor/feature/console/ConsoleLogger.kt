package com.scizor.feature.console

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Captures the host app's Logcat output into an in-memory rolling buffer.
 *
 * Reads `logcat` scoped to this process and parses the `threadtime` format.
 * All I/O is guarded — on devices where Logcat access is restricted, the logger
 * simply stays empty rather than crashing the host.
 */
object ConsoleLogger {

    private const val MAX_ENTRIES = 5000

    private val buffer = ArrayDeque<LogEntry>(MAX_ENTRIES)
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var readJob: Job? = null
    private var process: java.lang.Process? = null

    fun start() {
        if (readJob?.isActive == true) return
        readJob = scope.launch {
            runCatching {
                val pid = android.os.Process.myPid().toString()
                val proc = ProcessBuilder(
                    "logcat", "-v", "threadtime", "--pid", pid,
                ).redirectErrorStream(true).start()
                process = proc
                BufferedReader(InputStreamReader(proc.inputStream)).useLines { lines ->
                    for (line in lines) {
                        parseLine(line)?.let { append(it) }
                    }
                }
            }
        }
    }

    fun stop() {
        readJob?.cancel()
        readJob = null
        runCatching { process?.destroy() }
        process = null
    }

    fun clear() {
        synchronized(buffer) {
            buffer.clear()
            _entries.value = emptyList()
        }
    }

    /** Appends an entry, evicting the oldest once [MAX_ENTRIES] is exceeded. */
    internal fun append(entry: LogEntry) {
        synchronized(buffer) {
            buffer.addLast(entry)
            while (buffer.size > MAX_ENTRIES) {
                buffer.removeFirst()
            }
            _entries.value = buffer.toList()
        }
    }

    private val LINE = Regex(
        """^(\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})\s+\d+\s+\d+\s+([VDIWEAF])\s+(.*?):\s?(.*)$""",
    )

    /** Parses one `threadtime` Logcat line, or returns null if it doesn't match. */
    internal fun parseLine(line: String): LogEntry? {
        val match = LINE.matchEntire(line.trim()) ?: return null
        val (time, level, tag, message) = match.destructured
        return LogEntry(
            time = time,
            level = LogLevel.from(level.first()),
            tag = tag.trim(),
            message = message,
        )
    }
}
