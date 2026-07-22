package com.scizor.feature.network

import org.json.JSONArray
import org.json.JSONObject

/** JSON detection + pretty-printing for body views. */
internal object Json {

    fun looksLikeJson(contentType: String?, text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        if (contentType?.contains("json", ignoreCase = true) == true) return true
        val t = text.trim()
        return t.startsWith("{") || t.startsWith("[")
    }

    fun pretty(text: String): String = runCatching {
        val t = text.trim()
        when {
            t.startsWith("{") -> JSONObject(t).toString(2)
            t.startsWith("[") -> JSONArray(t).toString(2)
            else -> text
        }
    }.getOrDefault(text)
}
