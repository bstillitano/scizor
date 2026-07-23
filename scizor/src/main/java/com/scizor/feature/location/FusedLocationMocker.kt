package com.scizor.feature.location

import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

/**
 * Mocks the Google Play Services fused location provider so apps using
 * [FusedLocationProviderClient] (and Google Maps' "My Location") follow the spoof,
 * not just apps reading `LocationManager` directly.
 *
 * `play-services-location` is `compileOnly`, so [available] must gate every call —
 * on apps that don't ship it (or non-GMS devices) fused mocking is simply skipped
 * and the `LocationManager` test providers still apply. Like fused mocking anywhere,
 * it also requires this app to be the device's selected mock-location app.
 */
internal object FusedLocationMocker {

    /** True when `play-services-location` is on the runtime classpath. */
    val available: Boolean by lazy {
        runCatching {
            Class.forName("com.google.android.gms.location.LocationServices")
            true
        }.getOrDefault(false)
    }

    private var client: Any? = null

    /** Enters mock mode on the fused provider. Returns true if it was enabled. */
    fun start(context: Context): Boolean {
        if (!available) return false
        return runCatching {
            val c = LocationServices.getFusedLocationProviderClient(context.applicationContext)
            c.setMockMode(true)
            client = c
            true
        }.getOrDefault(false)
    }

    /** Pushes a fix to the fused provider. No-op unless mock mode was enabled. */
    fun push(location: Location) {
        if (!available) return
        val c = client as? FusedLocationProviderClient ?: return
        runCatching { c.setMockLocation(location) }
    }

    /** Leaves mock mode. */
    fun stop() {
        if (!available) return
        val c = client as? FusedLocationProviderClient ?: return
        runCatching { c.setMockMode(false) }
        client = null
    }
}
