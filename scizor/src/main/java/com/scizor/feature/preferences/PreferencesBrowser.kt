package com.scizor.feature.preferences

import android.content.Context
import java.io.File

/** A single SharedPreferences entry. */
data class PrefEntry(
    val key: String,
    val value: String,
    val type: String,
)

/**
 * Reads and edits the host app's [android.content.SharedPreferences] files.
 * Pure data access so it can be unit tested without UI.
 */
object PreferencesBrowser {

    /** Names (without the `.xml` suffix) of every SharedPreferences file. */
    fun files(context: Context): List<String> {
        val dir = File(context.applicationInfo.dataDir, "shared_prefs")
        val names = dir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".xml") }
            ?.map { it.name.removeSuffix(".xml") }
            ?: emptyList()
        return names.sorted()
    }

    fun entries(context: Context, file: String): List<PrefEntry> {
        val prefs = context.getSharedPreferences(file, Context.MODE_PRIVATE)
        return prefs.all.map { (key, value) ->
            PrefEntry(
                key = key,
                value = value?.toString() ?: "null",
                type = value?.javaClass?.simpleName ?: "null",
            )
        }.sortedBy { it.key }
    }

    fun putString(context: Context, file: String, key: String, value: String) {
        context.getSharedPreferences(file, Context.MODE_PRIVATE)
            .edit().putString(key, value).apply()
    }

    fun putBoolean(context: Context, file: String, key: String, value: Boolean) {
        context.getSharedPreferences(file, Context.MODE_PRIVATE)
            .edit().putBoolean(key, value).apply()
    }

    fun remove(context: Context, file: String, key: String) {
        context.getSharedPreferences(file, Context.MODE_PRIVATE)
            .edit().remove(key).apply()
    }
}
