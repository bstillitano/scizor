package com.scizor.feature.network

import com.apollographql.apollo.api.http.ByteStringHttpBody
import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpInterceptorChain
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScizorApolloInterceptorTest {

    @Before
    fun setUp() = NetworkLogger.clear()

    @After
    fun tearDown() = NetworkLogger.clear()

    private fun chain(handler: suspend (HttpRequest) -> HttpResponse) =
        object : HttpInterceptorChain {
            override suspend fun proceed(request: HttpRequest): HttpResponse = handler(request)
        }

    private fun graphQlRequest(body: String): HttpRequest =
        HttpRequest.Builder(HttpMethod.Post, "https://example.com/graphql")
            .addHeader("X-Custom", "yes")
            .body(ByteStringHttpBody("application/json", body))
            .build()

    private fun transaction(): NetworkTransaction {
        val tx = NetworkLogger.transactions.value.firstOrNull()
        assertNotNull("No transaction was recorded", tx)
        return tx!!
    }

    @Test
    fun `graphql request is recorded and the caller still reads the intact body`() = runBlocking {
        val requestJson =
            """{"query":"query GetUser { user { id } }","variables":{"id":1}}"""
        val responseJson = """{"data":{"user":{"id":1}}}"""

        val response = scizorApolloInterceptor().intercept(
            graphQlRequest(requestJson),
            chain {
                HttpResponse.Builder(200)
                    .addHeader("Content-Type", "application/json")
                    .body(Buffer().writeUtf8(responseJson))
                    .build()
            },
        )

        // The host reads the body after Scizor observed it — proof the stream is intact.
        assertEquals(responseJson, response.body!!.readUtf8())

        val tx = transaction()
        assertEquals("POST", tx.method)
        assertEquals("https://example.com/graphql", tx.url)
        assertEquals(200, tx.status)
        assertEquals(requestJson, tx.requestBody)
        assertEquals(responseJson, tx.responseBody)
        assertEquals(responseJson.length, tx.responseBytes)
        assertEquals("application/json", tx.contentType)
        assertEquals("yes", tx.requestHeaders["X-Custom"])
        assertNotNull(tx.durationMs)
        assertTrue(tx.timestamp > 0L)
        assertNull(tx.error)
        assertTrue(tx.isGraphQL)
        assertEquals("GetUser", tx.operationName)
        assertEquals("Query", tx.operationType)
        assertEquals("""{"id":1}""", tx.variables)
    }

    @Test
    fun `transport failure is recorded and rethrown`(): Unit = runBlocking {
        try {
            scizorApolloInterceptor().intercept(
                graphQlRequest("""{"query":"mutation Save { save }"}"""),
                chain { throw IOException("connection reset") },
            )
            fail("Expected the failure to propagate")
        } catch (expected: IOException) {
            assertEquals("connection reset", expected.message)
        }

        val tx = transaction()
        assertEquals("POST", tx.method)
        assertEquals("https://example.com/graphql", tx.url)
        assertNull(tx.status)
        assertEquals("connection reset", tx.error)
        assertNull(tx.responseBody)
    }

    @Test
    fun `response larger than the capture cap reaches the caller in full`() = runBlocking {
        val big = "x".repeat(1_500_000)

        val response = scizorApolloInterceptor().intercept(
            graphQlRequest("""{"query":"query Big { blob }"}"""),
            chain {
                HttpResponse.Builder(200)
                    .addHeader("Content-Type", "text/plain")
                    .body(Buffer().writeUtf8(big))
                    .build()
            },
        )

        assertEquals(big, response.body!!.readUtf8())

        val tx = transaction()
        assertEquals(200, tx.status)
        // Scizor keeps at most the cap; the host got everything regardless.
        assertEquals(1_000_000, tx.responseBody!!.length)
    }
}
