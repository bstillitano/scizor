package com.scizor.feature.crashlogs

import android.content.Context
import android.os.Build
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
    val appVersion: String,
    val osVersion: String,
    val device: String,
    val stackTrace: String,
) {
    /** The full copy/share report — header, environment, then stack trace. */
    fun fullReport(): String = buildString {
        appendLine("Crash Report")
        appendLine("============")
        appendLine()
        appendLine("Exception: $type")
        appendLine("Reason: ${message.ifBlank { "—" }}")
        appendLine("Thread: ${threadName.ifBlank { "—" }}")
        appendLine()
        appendLine("Device: $device")
        appendLine("OS: $osVersion")
        appendLine("App: $appVersion")
        appendLine()
        appendLine("Stack Trace:")
        append(stackTrace)
    }
}

/**
 * Captures uncaught exceptions and persists them to the app sandbox, along with
 * app/device environment cached at install time. Chains to the host's existing
 * handler and keeps at most [MAX_CRASHES] records.
 */
internal object CrashLogger {

    private const val DIR = "scizor_crashes"
    private const val MAX_CRASHES = 50

    private var appContext: Context? = null
    private var installed = false
    private var previous: Thread.UncaughtExceptionHandler? = null

    private var appVersion = "—"
    private var osVersion = "—"
    private var device = "—"

    fun install(context: Context) {
        appContext = context.applicationContext
        cacheEnvironment(context)
        if (installed) return
        installed = true
        previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { persist(thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun cacheEnvironment(context: Context) {
        osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        device = "${Build.MANUFACTURER} ${Build.MODEL}"
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
            appVersion = "${info.versionName} ($code)"
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
                appendLine(appVersion)
                appendLine(osVersion)
                appendLine(device)
                append(trace)
            },
        )
        trimToMax(dir)
    }

    private fun trimToMax(dir: File) {
        val files = dir.listFiles()?.filter { it.extension == "txt" }?.sortedByDescending { it.name } ?: return
        files.drop(MAX_CRASHES).forEach { it.delete() }
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
                        appVersion = lines.getOrElse(4) { "—" },
                        osVersion = lines.getOrElse(5) { "—" },
                        device = lines.getOrElse(6) { "—" },
                        stackTrace = lines.drop(7).joinToString("\n"),
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
        cacheEnvironment(context)
        runCatching { persist(Thread.currentThread(), throwable) }
    }
}
