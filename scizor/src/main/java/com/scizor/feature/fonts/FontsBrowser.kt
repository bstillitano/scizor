package com.scizor.feature.fonts

import android.content.Context
import java.io.File
import java.util.Locale

/** A font file. [isAsset] fonts load via `Typeface.createFromAsset`, others from a file path. */
internal data class FontInfo(
    val name: String,
    val path: String,
    val isAsset: Boolean = false,
)

/** A group of fonts sharing a family name. */
internal data class FontFamily(val name: String, val fonts: List<FontInfo>)

/** Lists system fonts (grouped into families) and any fonts bundled in the app's assets. */
internal object FontsBrowser {

    private val styleWords = setOf(
        "regular", "bold", "italic", "light", "medium", "thin", "black",
        "semibold", "extrabold", "condensed", "oblique", "book", "heavy",
    )

    /** System fonts from `/system/fonts`, grouped by derived family name. */
    fun families(): List<FontFamily> = runCatching {
        File("/system/fonts").listFiles()
            ?.filter { it.isFile && (it.extension.equals("ttf", true) || it.extension.equals("otf", true)) }
            ?.map { FontInfo(it.nameWithoutExtension, it.absolutePath) }
            ?.groupBy { familyOf(it.name) }
            ?.map { (family, fonts) -> FontFamily(family, fonts.sortedBy { it.name.lowercase() }) }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }.getOrDefault(emptyList())

    /** Fonts bundled in the app's `assets/` (root and `assets/fonts/`). */
    fun appFonts(context: Context): List<FontInfo> = runCatching {
        val dirs = listOf("", "fonts")
        dirs.flatMap { dir ->
            context.assets.list(dir).orEmpty()
                .filter { it.endsWith(".ttf", true) || it.endsWith(".otf", true) }
                .map { file ->
                    val path = if (dir.isEmpty()) file else "$dir/$file"
                    FontInfo(file.substringBeforeLast('.'), path, isAsset = true)
                }
        }.sortedBy { it.name.lowercase() }
    }.getOrDefault(emptyList())

    /** Strips a trailing style word ("Roboto-Bold" → "Roboto"). */
    private fun familyOf(name: String): String {
        val base = name.substringBefore('-').substringBefore('_')
        val tail = name.substringAfter('-', "").substringAfter('_', "")
        return if (tail.lowercase(Locale.ROOT) in styleWords || base.isNotBlank()) base else name
    }
}
