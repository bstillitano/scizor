package com.scizor.feature.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurlTest {

    private fun transaction(
        method: String,
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
    ) = NetworkTransaction(
        id = 1,
        method = method,
        url = url,
        requestHeaders = headers,
        requestBody = body,
        status = 200,
        responseHeaders = emptyMap(),
        responseBody = null,
        durationMs = 10,
        timestamp = 0,
    )

    @Test
    fun `get produces a bare curl with the url`() {
        val curl = transaction("GET", "https://example.com/x").toCurl()
        assertEquals("curl 'https://example.com/x'", curl)
    }

    @Test
    fun `post includes method header and body`() {
        val curl = transaction(
            method = "POST",
            url = "https://example.com/create",
            headers = mapOf("Content-Type" to "application/json"),
            body = """{"a":1}""",
        ).toCurl()

        assertTrue(curl.contains("-X POST"))
        assertTrue(curl.contains("-H 'Content-Type: application/json'"))
        assertTrue(curl.contains("""-d '{"a":1}'"""))
        assertTrue(curl.trimEnd().endsWith("'https://example.com/create'"))
    }

    @Test
    fun `single quotes in body are escaped`() {
        val curl = transaction(
            method = "POST",
            url = "https://example.com",
            body = "it's",
        ).toCurl()
        assertTrue(curl.contains("""-d 'it'\''s'"""))
    }
}
