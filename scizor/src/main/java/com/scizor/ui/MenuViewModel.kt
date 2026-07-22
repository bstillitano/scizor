package com.scizor.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.lifecycle.ViewModel
import com.scizor.Scizor
import com.scizor.core.FeatureRegistry

/**
 * Builds the grouped menu structure shown by [MenuScreen] from the registered
 * feature screens plus any custom developer options set on [Scizor].
 */
internal class MenuViewModel : ViewModel() {

    fun groups(): List<MenuGroupUi> {
        val groups = mutableListOf<MenuGroupUi>()

        FeatureRegistry.all()
            .groupBy { it.section }
            .forEach { (section, entries) ->
                groups.add(
                    MenuGroupUi(
                        title = section,
                        items = entries.map { entry ->
                            MenuItemUi(
                                id = entry.id,
                                title = entry.title,
                                subtitle = entry.subtitle,
                                icon = entry.icon,
                                action = MenuAction.Open(entry.title, entry.screen),
                            )
                        },
                    ),
                )
            }

        val developerOptions = Scizor.developerOptions
        if (developerOptions.isNotEmpty()) {
            groups.add(
                MenuGroupUi(
                    title = "Developer",
                    items = developerOptions.map { option ->
                        MenuItemUi(
                            id = "developer_option_${option.title}",
                            title = option.title,
                            subtitle = null,
                            icon = option.icon ?: Icons.Filled.Extension,
                            action = MenuAction.Run(option.onClick),
                        )
                    },
                ),
            )
        }

        return groups
    }
}
