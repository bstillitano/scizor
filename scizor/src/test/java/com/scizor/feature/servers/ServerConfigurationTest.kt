package com.scizor.feature.servers

import com.scizor.Scizor
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ServerConfigurationTest {

    private val dev = ServerEnvironment("Dev", "https://dev.example.com")
    private val staging = ServerEnvironment("Staging", "https://staging.example.com")
    private val prod = ServerEnvironment("Prod", "https://example.com")

    @Before
    fun setUp() {
        Scizor.start(RuntimeEnvironment.getApplication())
        // Reset persisted selection so tests are order-independent.
        Scizor.storeOrNull()?.remove("selected_server")
        ServerConfiguration.configure(listOf(dev, staging, prod))
    }

    @Test
    fun `defaults to first environment when none selected`() {
        assertEquals(dev.name, ServerConfiguration.selected?.name)
        assertEquals(dev.baseUrl, ServerConfiguration.baseUrl())
    }

    @Test
    fun `selection persists and drives baseUrl`() {
        ServerConfiguration.select(staging)
        assertEquals(staging.name, ServerConfiguration.selected?.name)
        assertEquals(staging.baseUrl, ServerConfiguration.baseUrl())
    }
}
