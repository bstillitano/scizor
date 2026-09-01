package com.scizor.feature.network

import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.charset
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.util.AttributeKey
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray

private const val MAX_BODY_BYTES = 1_000_000L

/**
 * Marks a request whose response reached the receive pipeline, so the [Send] error
 * path does not log a second, statusless transaction when a downstream plugin
 * (`expectSuccess`, for example) throws *after* the response was already recorded.
 */
private val ResponseSeenKey = AttributeKey<Unit>("ScizorResponseSeen")

/**
 * Carries the request timeout from the send side, where [HttpRequestBuilder] exposes
 * engine capabilities, to the response side, where only [io.ktor.util.Attributes] is public.
 */
private val TimeoutMillisKey = AttributeKey<Long>("ScizorRequestTimeoutMillis")

/**
 * Ktor client plugin that records every request/response into [NetworkLogger], for
 * hosts whose Ktor client runs on an engine other than OkHttp (CIO, Android, Java).
 *
 * The response body is captured through Ktor's own [ResponseObserver], which tees the
 * response channel in two: the host keeps an untouched, unread [io.ktor.utils.io.ByteReadChannel]
 * while Scizor reads the duplicate. Nothing here consumes, buffers, or replaces the
 * stream the caller receives, and every capture step is wrapped in `runCatching` so a
 * recording failure can never surface in the host's request.
 *
 * `ktor-client-core` is a `compileOnly` dependency, so this file is only ever reached
 * by apps that already ship Ktor.
 */
internal fun scizorKtorPlugin(): ClientPlugin<Unit> =
    createClientPlugin("ScizorNetworkLogger") {
        // Runs synchronously on the receive pipeline, before the Send hook below can
        // observe a downstream failure, so the flag is always set in time.
        onResponse { response ->
            runCatching { response.call.request.attributes.put(ResponseSeenKey, Unit) }
        }

        on(Send) { request ->
            runCatching {
                request.getCapabilityOrNull(HttpTimeoutCapability)?.requestTimeoutMillis
                    ?.let { request.attributes.put(TimeoutMillisKey, it) }
            }
            try {
                proceed(request)
            } catch (cause: Throwable) {
                if (!request.attributes.contains(ResponseSeenKey)) {
                    runCatching { recordFailure(request, cause) }
                }
                throw cause
            }
        }

        // ResponseObserver splits the response channel, hands the host one half and the
        // observer the other, then drains the observer's half once the handler returns.
        // Installing it directly is how Ktor's own Logging plugin observes bodies.
        val observer = ResponseObserver.prepare {
            onResponse { response -> runCatching { recordResponse(response) } }
        }
        ResponseObserver.install(observer, client)
    }

private suspend fun recordResponse(response: HttpResponse) {
    val request = response.call.request
    val url = runCatching { request.url.toString() }.getOrDefault("")
    val requestHeaders = runCatching { request.headers.toSimpleMap() }.getOrDefault(emptyMap())
    val requestBody = runCatching { readOutgoingBody(request.content) }.getOrNull()
    val contentType = runCatching { response.headers[HttpHeaders.ContentType] }.getOrNull()
    val isImage = contentType?.startsWith("image/", ignoreCase = true) == true

    val bytes = runCatching { readCapped(response) }.getOrNull()
    val responseBody = if (isImage) {
        null
    } else {
        bytes?.let { runCatching { String(it, response.charsetOrUtf8()) }.getOrNull() }
    }
    val responseImageBase64 = if (isImage && bytes != null) {
        runCatching {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }.getOrNull()
    } else {
        null
    }
    val graphql = runCatching { GraphQL.parse(url, requestBody) }.getOrNull()
    val timestamp = runCatching { response.requestTime.timestamp }.getOrDefault(System.currentTimeMillis())
    val durationMs = runCatching {
        (response.responseTime.timestamp - response.requestTime.timestamp).coerceAtLeast(0L)
    }.getOrNull()

    NetworkLogger.record(
        NetworkTransaction(
            id = NetworkLogger.nextId(),
            method = runCatching { request.method.value }.getOrDefault(""),
            url = url,
            requestHeaders = requestHeaders,
            requestBody = requestBody,
            status = runCatching { response.status.value }.getOrNull(),
            responseHeaders = runCatching { response.headers.toSimpleMap() }.getOrDefault(emptyMap()),
            responseBody = responseBody,
            durationMs = durationMs,
            timestamp = timestamp,
            contentType = contentType,
            cacheControl = runCatching { requestHeaders[HttpHeaders.CacheControl] }.getOrNull(),
            timeoutMs = runCatching { request.attributes.getOrNull(TimeoutMillisKey) }.getOrNull(),
            responseBytes = bytes?.size,
            isGraphQL = graphql != null,
            operationName = graphql?.operationName,
            operationType = graphql?.operationType,
            variables = graphql?.variables,
            responseImageBase64 = responseImageBase64,
        ),
    )
}

private fun recordFailure(request: HttpRequestBuilder, cause: Throwable) {
    val requestHeaders = runCatching { request.headers.toSimpleMap() }.getOrDefault(emptyMap())
    NetworkLogger.record(
        NetworkTransaction(
            id = NetworkLogger.nextId(),
            method = runCatching { request.method.value }.getOrDefault(""),
            url = runCatching { Url(request.url).toString() }.getOrDefault(""),
            requestHeaders = requestHeaders,
            requestBody = runCatching { readOutgoingBody(request.body) }.getOrNull(),
            status = null,
            responseHeaders = emptyMap(),
            responseBody = null,
            durationMs = null,
            timestamp = System.currentTimeMillis(),
            error = cause.message ?: cause.javaClass.simpleName,
            cacheControl = requestHeaders[HttpHeaders.CacheControl],
        ),
    )
}

/**
 * Reads at most [MAX_BODY_BYTES] from the observer's copy of the response. On the
 * streaming path this channel is the duplicate produced by [ResponseObserver]; on the
 * saved path (`HttpCache`, `call.save()`) `bodyAsChannel()` hands back a fresh reader
 * over the in-memory bytes. Neither is the channel the host reads from.
 */
private suspend fun readCapped(response: HttpResponse): ByteArray =
    response.bodyAsChannel().readRemaining(MAX_BODY_BYTES).readByteArray()

/**
 * Ktor's equivalent of OkHttp's `isDuplex()`/`isOneShot()` guard: only fully-buffered
 * request bodies can be read back here. Streaming bodies are left alone and reported
 * as `null` rather than risking a second read of a one-shot channel.
 */
private fun readOutgoingBody(body: Any?): String? {
    var content = body as? OutgoingContent ?: return null
    while (content is OutgoingContent.ContentWrapper) {
        content = content.delegate()
    }
    val bytes = (content as? OutgoingContent.ByteArrayContent)?.bytes() ?: return null
    if (bytes.isEmpty()) return null
    val charset = runCatching { content.contentType?.charset() }.getOrNull() ?: Charsets.UTF_8
    return if (bytes.size > MAX_BODY_BYTES) {
        String(bytes, 0, MAX_BODY_BYTES.toInt(), charset) + "… (truncated)"
    } else {
        String(bytes, charset)
    }
}

private fun HttpResponse.charsetOrUtf8(): java.nio.charset.Charset =
    runCatching { contentType()?.charset() }.getOrNull() ?: Charsets.UTF_8

private fun io.ktor.http.Headers.toSimpleMap(): Map<String, String> =
    entries().associate { (name, values) -> name to values.joinToString(", ") }

private fun io.ktor.util.StringValuesBuilder.toSimpleMap(): Map<String, String> =
    names().associateWith { name -> getAll(name)?.joinToString(", ").orEmpty() }
