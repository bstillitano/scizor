package com.scizor.feature.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.Interceptor

/** No-op mirror of the real [NetworkTransaction]. */
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
) {
    val host: String
        get() = runCatching { java.net.URI(url).host ?: url }.getOrDefault(url)

    val path: String
        get() = runCatching { java.net.URI(url).path?.ifEmpty { "/" } ?: "/" }.getOrDefault(url)
}

/** No-op interceptor: passes the request straight through, recording nothing. */
class ScizorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response =
        chain.proceed(chain.request())
}

/** No-op mirror of the real [NetworkLogger]. The interceptor is a pass-through. */
object NetworkLogger {
    val transactions: StateFlow<List<NetworkTransaction>> = MutableStateFlow(emptyList())
    fun interceptor(): Interceptor = ScizorInterceptor()
    fun clear() = Unit

    @Suppress("UNUSED_PARAMETER")
    fun find(id: Long): NetworkTransaction? = null
}
