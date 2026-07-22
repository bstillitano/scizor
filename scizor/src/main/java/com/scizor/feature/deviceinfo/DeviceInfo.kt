package com.scizor.feature.deviceinfo

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build

/** A single labelled fact shown on the device-info screen. */
data class InfoRow(val label: String, val value: String)

/**
 * Collects read-only device and application facts. Pure data gathering — no UI —
 * so it can be unit tested directly.
 */
object DeviceInfo {

    fun collect(context: Context): List<InfoRow> {
        val rows = mutableListOf<InfoRow>()

        rows += InfoRow("Manufacturer", Build.MANUFACTURER)
        rows += InfoRow("Model", Build.MODEL)
        rows += InfoRow("Device", Build.DEVICE)
        rows += InfoRow("Android Version", Build.VERSION.RELEASE)
        rows += InfoRow("API Level", Build.VERSION.SDK_INT.toString())

        val pm = context.packageManager
        val packageName = context.packageName
        rows += InfoRow("Package", packageName)

        runCatching {
            val info = pm.getPackageInfo(packageName, 0)
            rows += InfoRow("App Version", info.versionName ?: "—")
            rows += InfoRow("Build Number", versionCode(info).toString())
        }

        runCatching {
            val appInfo = context.applicationInfo
            val debuggable = (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            rows += InfoRow("Debuggable", debuggable.toString())
        }

        return rows
    }

    @Suppress("DEPRECATION")
    private fun versionCode(info: android.content.pm.PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            info.versionCode.toLong()
        }
    }
}
