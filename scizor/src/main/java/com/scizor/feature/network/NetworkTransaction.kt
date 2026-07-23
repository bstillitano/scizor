package com.scizor.feature.network

/** A captured HTTP request/response pair. */
data class NetworkTransaction(
    val id: Long,
    val method: String,
    val url: String,
    val requestHeaders: Map<String, String>,
    val requestBody: String?,
    val status: Int?,
    val responseHeaders: Map<String, String>,
    val responseBody: String?,
    val durationMs: Long?,
    val timestamp: Long,
    val error: String? = null,
    val contentType: String? = null,
    val cacheControl: String? = null,
    val timeoutMs: Long? = null,
    val responseBytes: Int? = null,
    val isGraphQL: Boolean = false,
    val operationName: String? = null,
    val operationType: String? = null,
    val variables: String? = null,
    /** Base64 (NO_WRAP) of an image response body, when the content type is an image. */
    val responseImageBase64: String? = null,
) {
    val host: String
        get() = runCatching { java.net.URI(url).host ?: url }.getOrDefault(url)

    val path: String
        get() = runCatching { java.net.URI(url).path?.ifEmpty { "/" } ?: "/" }.getOrDefault(url)
}
