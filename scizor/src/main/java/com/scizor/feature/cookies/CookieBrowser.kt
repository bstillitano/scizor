package com.scizor.feature.cookies

import com.scizor.feature.network.NetworkLogger

/** A cookie observed in captured HTTP traffic, with parsed attributes. */
internal data class Cookie(
    val name: String,
    val value: String,
    val host: String,
    val sent: Boolean,
    val path: String? = null,
    val domain: String? = null,
    val expires: String? = null,
    val httpOnly: Boolean = false,
    val secure: Boolean = false,
    val sameSite: String? = null,
)

/**
 * Surfaces cookies seen in the [NetworkLogger]'s captured traffic — request
 * `Cookie` headers (sent) and response `Set-Cookie` headers (received, with
 * attributes parsed). Android has no enumerable global cookie store, so this is
 * the practical analog to Scyther's HTTPCookieStorage view.
 */
internal object CookieBrowser {

    fun cookies(): List<Cookie> {
        val result = LinkedHashMap<String, Cookie>()
        NetworkLogger.transactions.value.forEach { tx ->
            tx.requestHeaders
                .filterKeys { it.equals("Cookie", ignoreCase = true) }
                .values
                .forEach { header ->
                    header.split(";").forEach { pair ->
                        parsePair(pair)?.let { (n, v) -> put(result, Cookie(n, v, tx.host, sent = true)) }
                    }
                }
            tx.responseHeaders
                .filterKeys { it.equals("Set-Cookie", ignoreCase = true) }
                .values
                .forEach { header -> parseSetCookie(header, tx.host)?.let { put(result, it) } }
        }
        return result.values.toList()
    }

    private fun parseSetCookie(header: String, host: String): Cookie? {
        val parts = header.split(";")
        val (name, value) = parsePair(parts.firstOrNull().orEmpty()) ?: return null
        var path: String? = null
        var domain: String? = null
        var expires: String? = null
        var httpOnly = false
        var secure = false
        var sameSite: String? = null
        parts.drop(1).forEach { raw ->
            val attr = raw.trim()
            when {
                attr.equals("HttpOnly", true) -> httpOnly = true
                attr.equals("Secure", true) -> secure = true
                attr.startsWith("Path=", true) -> path = attr.substringAfter("=")
                attr.startsWith("Domain=", true) -> domain = attr.substringAfter("=")
                attr.startsWith("Expires=", true) -> expires = attr.substringAfter("=")
                attr.startsWith("Max-Age=", true) -> expires = "${attr.substringAfter("=")}s"
                attr.startsWith("SameSite=", true) -> sameSite = attr.substringAfter("=")
            }
        }
        return Cookie(name, value, host, false, path, domain ?: host, expires, httpOnly, secure, sameSite)
    }

    private fun put(map: LinkedHashMap<String, Cookie>, cookie: Cookie) {
        map["${cookie.host}|${cookie.name}|${cookie.sent}"] = cookie
    }

    private fun parsePair(raw: String): Pair<String, String>? {
        val trimmed = raw.trim()
        val eq = trimmed.indexOf('=')
        if (eq <= 0) return null
        return trimmed.substring(0, eq).trim() to trimmed.substring(eq + 1).trim()
    }
}
