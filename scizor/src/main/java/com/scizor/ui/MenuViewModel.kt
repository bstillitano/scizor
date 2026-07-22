package com.scizor.ui

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.lifecycle.ViewModel
import com.scizor.Scizor
import com.scizor.core.FeatureRegistry
import com.scizor.feature.deviceinfo.DeviceInfo

/**
 * Builds the grouped menu shown by [MenuScreen]: inline Device and Application
 * facts (matching Scyther), followed by the registered feature screens and any
 * custom developer options.
 */
internal class MenuViewModel : ViewModel() {

    fun groups(context: Context): List<MenuGroupUi> {
        val groups = mutableListOf<MenuGroupUi>()

        val facts = DeviceInfo.collect(context)
        val device = facts.filter { it.label in DeviceInfo.deviceLabels }
        val application = facts.filterNot { it.label in DeviceInfo.deviceLabels }

        if (device.isNotEmpty()) {
            groups += MenuGroupUi(
                title = "Device",
                rows = device.map { MenuRow.Info("device_${it.label}", it.label, it.value) },
            )
        }
        if (application.isNotEmpty()) {
            groups += MenuGroupUi(
                title = "Application",
                rows = application.map { MenuRow.Info("app_${it.label}", it.label, it.value) },
            )
        }

        FeatureRegistry.all()
            .groupBy { it.section }
            .forEach { (section, entries) ->
                groups += MenuGroupUi(
                    title = section,
                    rows = entries.map { entry ->
                        MenuRow.Action(
                            id = entry.id,
                            title = entry.title,
                            subtitle = entry.subtitle,
                            icon = entry.icon,
                            action = MenuAction.Open(entry.title, entry.screen),
                        )
                    },
                )
            }

        val developerOptions = Scizor.developerOptions
        if (developerOptions.isNotEmpty()) {
            groups += MenuGroupUi(
                title = "Developer",
                rows = developerOptions.map { option ->
                    MenuRow.Action(
                        id = "developer_option_${option.title}",
                        title = option.title,
                        subtitle = null,
                        icon = option.icon ?: Icons.Filled.Extension,
                        action = MenuAction.Run(option.onClick),
                    )
                },
            )
        }

        return groups
    }
}
