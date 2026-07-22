package com.scizor.feature.fonts

import java.io.File

/** A system font file. */
internal data class FontInfo(val name: String, val path: String)

/** Lists the device's system font files from `/system/fonts`. */
internal object FontsBrowser {

    fun fonts(): List<FontInfo> = runCatching {
        File("/system/fonts").listFiles()
            ?.filter {
                it.isFile && (it.extension.equals("ttf", true) || it.extension.equals("otf", true))
            }
            ?.map { FontInfo(it.nameWithoutExtension, it.absolutePath) }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }.getOrDefault(emptyList())
}
