package com.scizor.feature.network

import com.apollographql.apollo.api.http.ByteStringHttpBody
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import okio.Buffer

private const val MAX_BODY_BYTES = 1_000_000L

/**
 * Apollo Kotlin 4 [HttpInterceptor] that records every GraphQL request/response into
 * [NetworkLogger], regardless of which HTTP engine the `ApolloClient` runs on.
 *
 * The request body is buffered once (when its length is known and within the cap) and
 * the request is rebuilt around the buffered bytes, so the original [com.apollographql.apollo.api.http.HttpBody]
 * is never written twice. The response body is observed through okio's `peek()`, which
 * reads a duplicate view of the source without consuming it — the host receives the
 * response object untouched, with its full body still readable. Every capture step is
 * wrapped in `runCatching` so a recording failure can never surface in the host's request.
 *
 * `apollo-runtime` is a `compileOnly` dependency, so this file is only ever reached by
 * apps that already ship Apollo.
 */
internal fun scizorApolloInterceptor(): HttpInterceptor = object : HttpInterceptor {
    override suspend fun intercept(
        request: HttpRequest,
        chain: HttpInterceptorChain,
    ): HttpResponse {
        val startMillis = System.currentTimeMillis()
        val requestBody = runCatching { readRequestBody(request) }.getOrNull()
        // Forward the buffered copy so the original body's writeTo runs exactly once.
        val forwarded = if (requestBody == null) {
            request
        } else {
            runCatching {
                request.newBuilder()
                    .body(ByteStringHttpBody(request.body?.contentType.orEmpty(), requestBody))
                    .build()
            }.getOrDefault(request)
        }

        val response = try {
            chain.proceed(forwarded)
        } catch (cause: Throwable) {
            runCatching { recordFailure(request, requestBody, cause) }
            throw cause
        }
        runCatching { recordResponse(request, requestBody, response, startMillis) }
        return response
    }
}

private fun recordResponse(
    request: HttpRequest,
    requestBody: String?,
    response: HttpResponse,
    startMillis: Long,
) {
    val requestHeaders = runCatching { request.headers.toSimpleMap() }.getOrDefault(emptyMap())
    val responseHeaders = runCatching { response.headers.toSimpleMap() }.getOrDefault(emptyMap())
    val contentType = responseHeaders.entries
        .firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }?.value
    val isImage = contentType?.startsWith("image/", ignoreCase = true) == true

    val bytes = runCatching { peekCapped(response) }.getOrNull()
    val responseBody = if (isImage) {
        null
    } else {
        bytes?.let { runCatching { String(it, Charsets.UTF_8) }.getOrNull() }
    }
    val responseImageBase64 = if (isImage && bytes != null) {
        runCatching {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }.getOrNull()
    } else {
        null
    }
    val graphql = runCatching { GraphQL.parse(request.url, requestBody) }.getOrNull()

    NetworkLogger.record(
        NetworkTransaction(
            id = NetworkLogger.nextId(),
            method = runCatching { request.method.name.uppercase() }.getOrDefault(""),
            url = request.url,
            requestHeaders = requestHeaders,
            requestBody = requestBody,
            status = response.statusCode,
            responseHeaders = responseHeaders,
            responseBody = responseBody,
            durationMs = (System.currentTimeMillis() - startMillis).coerceAtLeast(0L),
            timestamp = startMillis,
            contentType = contentType,
            cacheControl = requestHeaders.entries
                .firstOrNull { it.key.equals("Cache-Control", ignoreCase = true) }?.value,
            responseBytes = bytes?.size,
            isGraphQL = graphql != null,
            operationName = graphql?.operationName,
            operationType = graphql?.operationType,
            variables = graphql?.variables,
            responseImageBase64 = responseImageBase64,
        ),
    )
}

private fun recordFailure(request: HttpRequest, requestBody: String?, cause: Throwable) {
    val requestHeaders = runCatching { request.headers.toSimpleMap() }.getOrDefault(emptyMap())
    val graphql = runCatching { GraphQL.parse(request.url, requestBody) }.getOrNull()
    NetworkLogger.record(
        NetworkTransaction(
            id = NetworkLogger.nextId(),
            method = runCatching { request.method.name.uppercase() }.getOrDefault(""),
            url = request.url,
            requestHeaders = requestHeaders,
            requestBody = requestBody,
            status = null,
            responseHeaders = emptyMap(),
            responseBody = null,
            durationMs = null,
            timestamp = System.currentTimeMillis(),
            error = cause.message ?: cause.javaClass.simpleName,
            cacheControl = requestHeaders.entries
                .firstOrNull { it.key.equals("Cache-Control", ignoreCase = true) }?.value,
            isGraphQL = graphql != null,
            operationName = graphql?.operationName,
            operationType = graphql?.operationType,
            variables = graphql?.variables,
        ),
    )
}

/**
 * Apollo's analogue of the OkHttp `isOneShot()` guard: only bodies with a known length
 * within the cap are buffered. An unknown or oversized length (a streaming upload, say)
 * is left alone and reported as `null` rather than buffered wholesale into memory.
 */
private fun readRequestBody(request: HttpRequest): String? {
    val body = request.body ?: return null
    if (body.contentLength < 0 || body.contentLength > MAX_BODY_BYTES) return null
    val buffer = Buffer()
    body.writeTo(buffer)
    return buffer.readUtf8().ifEmpty { null }
}

/**
 * Reads at most [MAX_BODY_BYTES] through `peek()`, a duplicate reader over the same
 * source. The peeked bytes stay buffered in the source, so the host later reads the
 * complete body from the start — nothing here consumes the stream it receives.
 */
private fun peekCapped(response: HttpResponse): ByteArray? {
    val source = response.body ?: return null
    val peek = source.peek()
    val buffer = Buffer()
    var remaining = MAX_BODY_BYTES
    while (remaining > 0) {
        val read = peek.read(buffer, remaining)
        if (read == -1L) break
        remaining -= read
    }
    return buffer.readByteArray()
}

private fun List<HttpHeader>.toSimpleMap(): Map<String, String> =
    groupBy({ it.name }, { it.value })
        .mapValues { (_, values) -> values.joinToString(", ") }
