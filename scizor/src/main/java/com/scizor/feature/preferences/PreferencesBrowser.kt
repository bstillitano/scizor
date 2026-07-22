package com.scizor.feature.preferences

import android.content.Context
import java.io.File

/** A single SharedPreferences entry with its detected type. */
data class PrefEntry(
    val key: String,
    val value: String,
    val type: String,
)

/**
 * Reads and edits the host app's [android.content.SharedPreferences] files with
 * per-type awareness (String, Boolean, Int, Long, Float, StringSet).
 */
object PreferencesBrowser {

    /** Names (without the `.xml` suffix) of every SharedPreferences file. */
    fun files(context: Context): List<String> {
        val dir = File(context.applicationInfo.dataDir, "shared_prefs")
        return dir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".xml") }
            ?.map { it.name.removeSuffix(".xml") }
            ?.sorted()
            ?: emptyList()
    }

    fun entries(context: Context, file: String): List<PrefEntry> {
        val prefs = context.getSharedPreferences(file, Context.MODE_PRIVATE)
        return prefs.all.map { (key, value) ->
            val (type, display) = describe(value)
            PrefEntry(key, display, type)
        }.sortedBy { it.key }
    }

    private fun describe(value: Any?): Pair<String, String> = when (value) {
        is Boolean -> "Boolean" to value.toString()
        is Int -> "Int" to value.toString()
        is Long -> "Long" to value.toString()
        is Float -> "Float" to value.toString()
        is String -> "String" to value
        is Set<*> -> "StringSet" to "${value.size} items"
        null -> "null" to "null"
        else -> (value.javaClass.simpleName ?: "Unknown") to value.toString()
    }

    fun stringSet(context: Context, file: String, key: String): List<String> =
        context.getSharedPreferences(file, Context.MODE_PRIVATE)
            .getStringSet(key, emptySet())
            ?.sorted()
            ?: emptyList()

    fun putString(context: Context, file: String, key: String, value: String) =
        edit(context, file) { putString(key, value) }

    fun putBoolean(context: Context, file: String, key: String, value: Boolean) =
        edit(context, file) { putBoolean(key, value) }

    fun putInt(context: Context, file: String, key: String, value: Int) =
        edit(context, file) { putInt(key, value) }

    fun putLong(context: Context, file: String, key: String, value: Long) =
        edit(context, file) { putLong(key, value) }

    fun putFloat(context: Context, file: String, key: String, value: Float) =
        edit(context, file) { putFloat(key, value) }

    fun remove(context: Context, file: String, key: String) =
        edit(context, file) { remove(key) }

    fun resetAll(context: Context, file: String) =
        edit(context, file) { clear() }

    private inline fun edit(
        context: Context,
        file: String,
        block: android.content.SharedPreferences.Editor.() -> Unit,
    ) {
        context.getSharedPreferences(file, Context.MODE_PRIVATE).edit().apply(block).apply()
    }
}
