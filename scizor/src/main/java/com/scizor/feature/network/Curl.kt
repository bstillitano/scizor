package com.scizor.feature.network

/**
 * Builds a runnable `curl` command that reproduces this transaction's request.
 * Single-quotes are escaped so the output is safe to paste into a shell.
 */
fun NetworkTransaction.toCurl(): String {
    val parts = mutableListOf("curl")

    if (!method.equals("GET", ignoreCase = true)) {
        parts += "-X $method"
    }

    requestHeaders.forEach { (name, value) ->
        parts += "-H ${quote("$name: $value")}"
    }

    requestBody?.takeIf { it.isNotEmpty() }?.let { body ->
        parts += "-d ${quote(body)}"
    }

    parts += quote(url)
    return parts.joinToString(" ")
}

private fun quote(value: String): String {
    val escaped = value.replace("'", "'\\''")
    return "'$escaped'"
}
