package com.scizor.feature.deeplink

import android.content.Context
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Thin wrapper over the Google Code Scanner. The dependency is `compileOnly`, so
 * [available] must be checked before [scan] — on apps that don't ship the scanner
 * (or on non-GMS devices) the button is simply hidden.
 */
internal object QrScanner {

    /** True when `play-services-code-scanner` is on the runtime classpath. */
    val available: Boolean by lazy {
        runCatching {
            Class.forName("com.google.mlkit.vision.codescanner.GmsBarcodeScanning")
            true
        }.getOrDefault(false)
    }

    /** Launches the system QR scanner UI; [onResult] receives the decoded string or null. */
    fun scan(context: Context, onResult: (String?) -> Unit) {
        runCatching {
            val options = GmsBarcodeScannerOptions.Builder().build()
            GmsBarcodeScanning.getClient(context, options)
                .startScan()
                .addOnSuccessListener { barcode -> onResult(barcode.rawValue) }
                .addOnCanceledListener { onResult(null) }
                .addOnFailureListener { onResult(null) }
        }.onFailure { onResult(null) }
    }
}
