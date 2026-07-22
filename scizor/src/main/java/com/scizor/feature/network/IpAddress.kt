package com.scizor.feature.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URL

/**
 * Fetches the device's public IP address once, asynchronously, for the menu's
 * Networking section (mirrors Scyther's IP Address row). Uses a raw connection
 * so it never runs through the host's OkHttp client / the network logger.
 */
internal object IpAddress {

    private val _value = MutableStateFlow<String?>(null)
    val value: StateFlow<String?> = _value.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    fun load() {
        if (started) return
        started = true
        scope.launch {
            _value.value = runCatching {
                URL("https://api.ipify.org").openStream()
                    .bufferedReader()
                    .use { it.readText().trim() }
            }.getOrNull()
        }
    }
}
