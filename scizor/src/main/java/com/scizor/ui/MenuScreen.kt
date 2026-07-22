package com.scizor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Root screen of the Scizor menu: a grouped, tappable list of every registered
 * debugging tool and custom developer option.
 */
@Composable
internal fun MenuScreen(
    navigator: ScizorNavigator,
    viewModel: MenuViewModel = viewModel(),
) {
    val groups = viewModel.groups()

    if (groups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No Scizor tools registered.\nCall Scizor.start() in your Application.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(24.dp),
            )
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        groups.forEach { group ->
            item(key = "header_${group.title}") {
                Text(
                    text = group.title.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
                )
            }
            items(group.items, key = { it.id }) { menuItem ->
                MenuRow(menuItem = menuItem, navigator = navigator)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun MenuRow(menuItem: MenuItemUi, navigator: ScizorNavigator) {
    ListItem(
        headlineContent = { Text(menuItem.title) },
        supportingContent = menuItem.subtitle?.let { { Text(it) } },
        leadingContent = {
            Icon(imageVector = menuItem.icon, contentDescription = null)
        },
        modifier = Modifier.clickable {
            when (val action = menuItem.action) {
                is MenuAction.Open -> navigator.push(action.title, action.screen)
                is MenuAction.Run -> action.block()
            }
        },
    )
}
