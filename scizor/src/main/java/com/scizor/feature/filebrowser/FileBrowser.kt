package com.scizor.feature.filebrowser

import java.io.File

/** Broad file categories, for iconography and content handling. */
internal enum class FileKind { DIRECTORY, TEXT, JSON, IMAGE, DATABASE, ARCHIVE, AUDIO, VIDEO, PDF, BINARY }

/** A file or directory in the app sandbox. */
internal data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val kind: FileKind,
) {
    val file: File get() = File(path)
}

/** A named sandbox root the browser starts from. */
internal data class FileRoot(val label: String, val path: String)

/** Read-only browser over the app's private sandbox. */
internal object FileBrowser {

    private const val MAX_WALK = 4000

    fun roots(context: android.content.Context): List<FileRoot> {
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
        return candidates.filter { it.second.exists() }.map { FileRoot(it.first, it.second.absolutePath) }
    }

    fun list(dir: File): List<FileNode> {
        val children = dir.listFiles() ?: return emptyList()
        return children
            .map { child ->
                FileNode(
                    name = child.name,
                    path = child.absolutePath,
                    isDirectory = child.isDirectory,
                    size = if (child.isDirectory) folderSize(child) else child.length(),
                    lastModified = child.lastModified(),
                    kind = kindOf(child),
                )
            }
            .sortedWith(compareByDescending<FileNode> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    private fun folderSize(dir: File): Long {
        var total = 0L
        var count = 0
        dir.walkTopDown().forEach {
            if (count++ > MAX_WALK) return total
            if (it.isFile) total += it.length()
        }
        return total
    }

    private fun kindOf(file: File): FileKind {
        if (file.isDirectory) return FileKind.DIRECTORY
        return when (file.extension.lowercase()) {
            "txt", "log", "xml", "json", "kt", "java", "md", "html", "css", "js", "yaml", "yml", "csv" ->
                if (file.extension.equals("json", true)) FileKind.JSON else FileKind.TEXT
            "png", "jpg", "jpeg", "gif", "webp", "bmp" -> FileKind.IMAGE
            "db", "sqlite", "sqlite3" -> FileKind.DATABASE
            "zip", "gz", "tar", "apk", "aar", "jar" -> FileKind.ARCHIVE
            "mp3", "wav", "aac", "ogg", "m4a" -> FileKind.AUDIO
            "mp4", "mkv", "webm", "mov" -> FileKind.VIDEO
            "pdf" -> FileKind.PDF
            else -> FileKind.BINARY
        }
    }

    fun readText(file: File, maxBytes: Long = 1_000_000): String? = runCatching {
        if (!file.isFile) return null
        if (file.length() > maxBytes) return "[File too large to display: ${humanSize(file.length())}]"
        file.readText()
    }.getOrNull()

    fun delete(file: File): Boolean = runCatching {
        if (file.isDirectory) file.deleteRecursively() else file.delete()
    }.getOrDefault(false)

    fun permissions(file: File): String = buildString {
        append(if (file.canRead()) "r" else "-")
        append(if (file.canWrite()) "w" else "-")
        append(if (file.canExecute()) "x" else "-")
    }

    fun humanSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
