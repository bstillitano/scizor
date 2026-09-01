package com.scizor.feature.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScizorKtorPluginTest {

    @Before
    fun setUp() = NetworkLogger.clear()

    @After
    fun tearDown() = NetworkLogger.clear()

    private fun client(
        handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = HttpClient(MockEngine { request -> handler(request) }) {
        install(NetworkLogger.ktorPlugin())
    }

    /** ResponseObserver records asynchronously; wait briefly for the transaction to land. */
    private fun awaitTransaction(): NetworkTransaction {
        repeat(200) {
            NetworkLogger.transactions.value.firstOrNull()?.let { return it }
            Thread.sleep(10)
        }
        throw AssertionError("No transaction was recorded")
    }

    @Test
    fun `get request is recorded and the caller still reads the body`() = runBlocking {
        val client = client {
            respond(
                content = "ok",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/plain"),
            )
        }

        // The host reads the body after Scizor observed it — proof the stream is intact.
        val body = client.get("https://example.com/hello").bodyAsText()
        assertEquals("ok", body)

        val tx = awaitTransaction()
        assertEquals("GET", tx.method)
        assertEquals(200, tx.status)
        assertEquals("https://example.com/hello", tx.url)
        assertEquals("ok", tx.responseBody)
        assertEquals(2, tx.responseBytes)
        assertEquals("text/plain", tx.contentType)
        assertNotNull(tx.durationMs)
        assertTrue(tx.timestamp > 0L)
        assertNull(tx.error)
    }

    @Test
    fun `post body, headers and graphql details are captured`() = runBlocking {
        val client = client { respond("{}", HttpStatusCode.Created) }

        client.post("https://example.com/graphql") {
            header("X-Trace", "abc")
            setBody(
                TextContent(
                    """{"query":"query GetUser { user { id } }","variables":{"id":1}}""",
                    ContentType.Application.Json,
                ),
            )
        }.bodyAsText()

        val tx = awaitTransaction()
        assertEquals("POST", tx.method)
        assertEquals(201, tx.status)
        assertEquals("abc", tx.requestHeaders["X-Trace"])
        assertTrue(tx.requestBody!!.contains("GetUser"))
        assertTrue(tx.isGraphQL)
        assertEquals("GetUser", tx.operationName)
        assertEquals("Query", tx.operationType)
        assertEquals("""{"id":1}""", tx.variables)
    }

    @Test
    fun `an engine failure is recorded and rethrown to the caller`() = runBlocking {
        val client = client { throw java.io.IOException("boom") }

        val thrown = runCatching { client.get("https://example.com/fail") }.exceptionOrNull()
        assertNotNull(thrown)

        val tx = awaitTransaction()
        assertEquals("GET", tx.method)
        assertEquals("https://example.com/fail", tx.url)
        assertNull(tx.status)
        assertEquals("boom", tx.error)
    }

    @Test
    fun `a non-2xx response is recorded once, not also as an error`() = runBlocking {
        val client = HttpClient(MockEngine { respondError(HttpStatusCode.NotFound) }) {
            expectSuccess = true
            install(NetworkLogger.ktorPlugin())
        }

        runCatching { client.get("https://example.com/missing") }

        awaitTransaction()
        Thread.sleep(200)
        val all = NetworkLogger.transactions.value
        assertEquals(1, all.size)
        assertEquals(404, all.first().status)
    }
}
