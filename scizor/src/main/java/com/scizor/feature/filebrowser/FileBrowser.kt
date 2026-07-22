package com.scizor.feature.filebrowser

import android.content.Context
import java.io.File

/** A file or directory in the app sandbox. */
internal data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
) {
    val file: File get() = File(path)
}

/** A named sandbox root the browser starts from. */
internal data class FileRoot(val label: String, val path: String)

/**
 * Read-only browser over the app's private sandbox. Exposes the standard roots
 * (files, cache, databases, shared_prefs, external) and lists their contents.
 */
internal object FileBrowser {

    fun roots(context: Context): List<FileRoot> {
        val dataDir = context.applicationInfo.dataDir
        val candidates = buildList {
            add("Files" to context.filesDir)
            add("Cache" to context.cacheDir)
            add("Code Cache" to context.codeCacheDir)
            add("No Backup" to context.noBackupFilesDir)
            add("Databases" to File(dataDir, "databases"))
            add("Shared Prefs" to File(dataDir, "shared_prefs"))
            context.getExternalFilesDir(null)?.let { add("External Files" to it) }
            context.externalCacheDir?.let { add("External Cache" to it) }
        }
        return candidates
            .filter { it.second.exists() }
            .map { FileRoot(it.first, it.second.absolutePath) }
    }

    fun list(dir: File): List<FileNode> {
        val children = dir.listFiles() ?: return emptyList()
        return children
            .map {
                FileNode(
                    name = it.name,
                    path = it.absolutePath,
                    isDirectory = it.isDirectory,
                    size = if (it.isDirectory) 0L else it.length(),
                    lastModified = it.lastModified(),
                )
            }
            .sortedWith(compareByDescending<FileNode> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    /** Reads a file as text if it is within [maxBytes]; returns null when too large or unreadable. */
    fun readText(file: File, maxBytes: Long = 512 * 1024): String? = runCatching {
        if (!file.isFile || file.length() > maxBytes) return null
        file.readText()
    }.getOrNull()

    fun humanSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
