package com.scizor.feature.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScizorInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        NetworkLogger.clear()
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .addInterceptor(NetworkLogger.interceptor())
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
        NetworkLogger.clear()
    }

    @Test
    fun `get request is recorded with status and duration`() {
        server.enqueue(MockResponse().setBody("ok").setResponseCode(200))

        val request = Request.Builder().url(server.url("/hello")).build()
        client.newCall(request).execute().use { it.body?.string() }

        val tx = NetworkLogger.transactions.value.first()
        assertEquals("GET", tx.method)
        assertEquals(200, tx.status)
        assertTrue(tx.url.endsWith("/hello"))
        assertEquals("ok", tx.responseBody)
        assertNotNull(tx.durationMs)
    }

    @Test
    fun `post request body is captured`() {
        server.enqueue(MockResponse().setResponseCode(201))

        val body = """{"name":"scizor"}""".toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(server.url("/create")).post(body).build()
        client.newCall(request).execute().use { it.body?.string() }

        val tx = NetworkLogger.transactions.value.first()
        assertEquals("POST", tx.method)
        assertEquals(201, tx.status)
        assertEquals("""{"name":"scizor"}""", tx.requestBody)
    }
}
