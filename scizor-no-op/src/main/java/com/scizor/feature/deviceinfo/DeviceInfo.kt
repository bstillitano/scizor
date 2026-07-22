package com.scizor.feature.deviceinfo

import android.content.Context

/** No-op mirror of the real [InfoRow]. */
data class InfoRow(val label: String, val value: String)

/** No-op mirror of the real [DeviceInfo]. */
object DeviceInfo {
    @Suppress("UNUSED_PARAMETER")
    fun collect(context: Context): List<InfoRow> = emptyList()
}
