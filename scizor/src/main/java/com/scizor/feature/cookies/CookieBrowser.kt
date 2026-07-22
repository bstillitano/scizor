package com.scizor.feature.cookies

import com.scizor.feature.network.NetworkLogger

/** A cookie observed in captured HTTP traffic. */
internal data class Cookie(
    val name: String,
    val value: String,
    val host: String,
    val sent: Boolean,
)

/**
 * Surfaces cookies seen in the [NetworkLogger]'s captured traffic — request
 * `Cookie` headers (sent) and response `Set-Cookie` headers (received). This is
 * zero-config: any request logged through Scizor's interceptor contributes.
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
                        parsePair(pair)?.let { (n, v) ->
                            put(result, Cookie(n, v, tx.host, sent = true))
                        }
                    }
                }
            tx.responseHeaders
                .filterKeys { it.equals("Set-Cookie", ignoreCase = true) }
                .values
                .forEach { header ->
                    val first = header.substringBefore(";")
                    parsePair(first)?.let { (n, v) ->
                        put(result, Cookie(n, v, tx.host, sent = false))
                    }
                }
        }
        return result.values.toList()
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
