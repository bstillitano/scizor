package com.scizor.feature.deviceinfo

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.text.DateFormat
import java.util.Date

/** A single labelled fact shown in the Device or Application section. */
data class InfoRow(val label: String, val value: String)

/**
 * Collects read-only device and application facts, mirroring the fields Scyther
 * surfaces on iOS (adapted to Android). Pure data gathering — no UI — so it can
 * be unit tested directly.
 */
object DeviceInfo {

    /** Labels that belong to the "Device" section; everything else is "Application". */
    val deviceLabels = setOf(
        "OS Version", "API Level", "Manufacturer", "Model", "Hardware", "Device ID",
    )

    fun collect(context: Context): List<InfoRow> {
        val rows = mutableListOf<InfoRow>()

        // Device
        rows += InfoRow("OS Version", "Android ${Build.VERSION.RELEASE}")
        rows += InfoRow("API Level", Build.VERSION.SDK_INT.toString())
        rows += InfoRow("Manufacturer", Build.MANUFACTURER)
        rows += InfoRow("Model", Build.MODEL)
        rows += InfoRow("Hardware", Build.HARDWARE)
        runCatching {
            val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (!id.isNullOrEmpty()) rows += InfoRow("Device ID", id)
        }

        // Application
        val pm = context.packageManager
        val packageName = context.packageName
        runCatching {
            rows += InfoRow("Name", pm.getApplicationLabel(context.applicationInfo).toString())
        }
        rows += InfoRow("Package", packageName)

        runCatching {
            val info = pm.getPackageInfo(packageName, 0)
            rows += InfoRow("Version", info.versionName ?: "—")
            rows += InfoRow("Build", versionCode(info).toString())
            rows += InfoRow(
                "Installed",
                DateFormat.getDateTimeInstance().format(Date(info.lastUpdateTime)),
            )
        }

        rows += InfoRow("Process ID", Process.myPid().toString())

        runCatching {
            val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            val base = if (debuggable) "Debug" else "Release"
            rows += InfoRow("Type", if (isEmulator()) "$base · Emulator" else base)
        }

        return rows
    }

    private fun isEmulator(): Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("emulator") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("sdk_gphone") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.PRODUCT.contains("sdk")

    @Suppress("DEPRECATION")
    private fun versionCode(info: android.content.pm.PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            info.versionCode.toLong()
        }
    }
}
