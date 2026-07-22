package com.scizor.feature.preferences

import android.content.Context

/** No-op mirror of the real [PrefEntry]. */
data class PrefEntry(
    val key: String,
    val value: String,
    val type: String,
)

/** No-op mirror of the real [PreferencesBrowser]. */
@Suppress("UNUSED_PARAMETER")
object PreferencesBrowser {
    fun files(context: Context): List<String> = emptyList()
    fun entries(context: Context, file: String): List<PrefEntry> = emptyList()
    fun putString(context: Context, file: String, key: String, value: String) = Unit
    fun putBoolean(context: Context, file: String, key: String, value: Boolean) = Unit
    fun remove(context: Context, file: String, key: String) = Unit
}
