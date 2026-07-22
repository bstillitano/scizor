package com.scizor.feature.network

import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer

/**
 * OkHttp interceptor that records every request/response into [NetworkLogger].
 * The response body is read via `peekBody` so the host app still receives an
 * unconsumed stream. Recording never alters or fails the actual call.
 */
class ScizorInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val id = NetworkLogger.nextId()
        val startNs = System.nanoTime()
        val timestamp = System.currentTimeMillis()
        val requestHeaders = request.headers.toSimpleMap()
        val requestBody = readRequestBody(request)

        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            NetworkLogger.record(
                NetworkTransaction(
                    id = id,
                    method = request.method,
                    url = request.url.toString(),
                    requestHeaders = requestHeaders,
                    requestBody = requestBody,
                    status = null,
                    responseHeaders = emptyMap(),
                    responseBody = null,
                    durationMs = (System.nanoTime() - startNs) / 1_000_000,
                    timestamp = timestamp,
                    error = e.message ?: e.javaClass.simpleName,
                ),
            )
            throw e
        }

        val durationMs = (System.nanoTime() - startNs) / 1_000_000
        val responseBody = runCatching {
            response.peekBody(MAX_BODY_BYTES).string()
        }.getOrNull()

        NetworkLogger.record(
            NetworkTransaction(
                id = id,
                method = request.method,
                url = request.url.toString(),
                requestHeaders = requestHeaders,
                requestBody = requestBody,
                status = response.code,
                responseHeaders = response.headers.toSimpleMap(),
                responseBody = responseBody,
                durationMs = durationMs,
                timestamp = timestamp,
            ),
        )
        return response
    }

    private fun readRequestBody(request: Request): String? {
        val body = request.body ?: return null
        if (body.isDuplex() || body.isOneShot()) return null
        return runCatching {
            val buffer = Buffer()
            body.writeTo(buffer)
            if (buffer.size > MAX_BODY_BYTES) {
                buffer.readUtf8(MAX_BODY_BYTES) + "… (truncated)"
            } else {
                buffer.readUtf8()
            }
        }.getOrNull()
    }

    private fun Headers.toSimpleMap(): Map<String, String> =
        (0 until size).associate { name(it) to value(it) }

    companion object {
        private const val MAX_BODY_BYTES = 1_000_000L
    }
}
