package com.scizor.feature.crashlogs

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/** A persisted uncaught-exception record. */
internal data class CrashLog(
    val id: String,
    val timestamp: Long,
    val type: String,
    val message: String,
    val threadName: String,
    val stackTrace: String,
)

/**
 * Captures uncaught exceptions and persists them to the app sandbox so they can
 * be reviewed on the next launch. Chains to the host's existing handler, so it
 * never suppresses the app's own crash reporting.
 */
internal object CrashLogger {

    private const val DIR = "scizor_crashes"

    private var appContext: Context? = null
    private var installed = false
    private var previous: Thread.UncaughtExceptionHandler? = null

    fun install(context: Context) {
        appContext = context.applicationContext
        if (installed) return
        installed = true
        previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { persist(thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun persist(thread: Thread, throwable: Throwable) {
        val context = appContext ?: return
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        val now = System.currentTimeMillis()
        val trace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        File(dir, "$now.txt").writeText(
            buildString {
                appendLine(now)
                appendLine(throwable.javaClass.name)
                appendLine(throwable.message?.replace("\n", " ") ?: "")
                appendLine(thread.name)
                append(trace)
            },
        )
    }

    fun crashes(context: Context): List<CrashLog> {
        val dir = File(context.filesDir, DIR)
        val files = dir.listFiles()?.filter { it.extension == "txt" } ?: return emptyList()
        return files
            .sortedByDescending { it.name }
            .mapNotNull { file ->
                runCatching {
                    val lines = file.readText().lines()
                    CrashLog(
                        id = file.name,
                        timestamp = lines.getOrNull(0)?.toLongOrNull() ?: 0L,
                        type = lines.getOrElse(1) { "" },
                        message = lines.getOrElse(2) { "" },
                        threadName = lines.getOrElse(3) { "" },
                        stackTrace = lines.drop(4).joinToString("\n"),
                    )
                }.getOrNull()
            }
    }

    fun clear(context: Context) {
        File(context.filesDir, DIR).listFiles()?.forEach { it.delete() }
    }

    /** Test/demo helper: records a synthetic crash without terminating the process. */
    fun recordForDemo(context: Context, throwable: Throwable) {
        appContext = context.applicationContext
        runCatching { persist(Thread.currentThread(), throwable) }
    }
}
