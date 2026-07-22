@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.scizor.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scizor.core.MenuPins
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
internal fun MenuScreen(
    navigator: ScizorNavigator,
    viewModel: MenuViewModel = viewModel(),
) {
    val context = LocalContext.current
    val ipAddress by com.scizor.feature.network.IpAddress.value.collectAsStateWithLifecycle()
    val pins by com.scizor.core.MenuPins.pins.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { com.scizor.feature.network.IpAddress.load() }
    val groups = viewModel.groups(context, ipAddress, pins)

    if (groups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No Scizor tools registered.\nCall Scizor.start() in your Application.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "app_header") { AppHeader() }

        groups.forEach { group ->
            item(key = "header_${group.title}") { Subheader(group.title) }
            item(key = "group_${group.title}") { SegmentedGroup(group.rows, navigator) }
        }
    }
}

@Composable
private fun Subheader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 8.dp),
    )
}

/** A section rendered with Material 3's expressive [SegmentedListItem] group. */
@Composable
private fun SegmentedGroup(rows: List<MenuRow>, navigator: ScizorNavigator) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        rows.forEachIndexed { index, row ->
            val shapes = ListItemDefaults.segmentedShapes(index = index, count = rows.size)
            when (row) {
                is MenuRow.Info -> InfoSegment(row, shapes)
                is MenuRow.Action -> ActionSegment(row, shapes, navigator)
                is MenuRow.Toggle -> ToggleSegment(row, shapes)
            }
        }
    }
}

@Composable
private fun InfoSegment(row: MenuRow.Info, shapes: androidx.compose.material3.ListItemShapes) {
    val clipboard = LocalClipboardManager.current
    SegmentedListItem(
        shapes = shapes,
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        leadingContent = row.icon?.let { icon ->
            { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        trailingContent = {
            if (row.value == LOADING_PLACEHOLDER) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = row.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        modifier = Modifier.combinedClickable(
            onClick = {},
            onLongClick = {
                clipboard.setText(AnnotatedString("${row.label}: ${row.value}"))
            },
        ),
        content = { Text(row.label) },
    )
}

/** Sentinel value a still-resolving [MenuRow.Info] carries; rendered as a spinner. */
internal const val LOADING_PLACEHOLDER = "Loading…"

@Composable
private fun ToggleSegment(row: MenuRow.Toggle, shapes: androidx.compose.material3.ListItemShapes) {
    val checked by row.flow.collectAsStateWithLifecycle()
    SegmentedListItem(
        shapes = shapes,
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        leadingContent = { LeadingIcon(row.icon) },
        supportingContent = row.subtitle?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = row.onChange) },
        modifier = Modifier.combinedClickable(onClick = { row.onChange(!checked) }, onLongClick = {}),
        content = { Text(row.title) },
    )
}

@Composable
private fun ActionSegment(
    row: MenuRow.Action,
    shapes: androidx.compose.material3.ListItemShapes,
    navigator: ScizorNavigator,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        SegmentedListItem(
            onClick = {
                when (val action = row.action) {
                    is MenuAction.Open -> navigator.push(action.title) { action.screen(navigator) }
                    is MenuAction.Run -> action.block()
                }
            },
            onLongClick = row.pinnableId?.let { { menuOpen = true } },
            shapes = shapes,
            colors = ListItemDefaults.segmentedColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            leadingContent = { LeadingIcon(row.icon) },
            supportingContent = row.subtitle?.let { { Text(it) } },
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            content = { Text(row.title) },
        )
        row.pinnableId?.let { pinId ->
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(if (MenuPins.isPinned(pinId)) "Unpin" else "Pin to top") },
                    onClick = {
                        MenuPins.toggle(pinId)
                        menuOpen = false
                    },
                )
            }
        }
    }
}

@Composable
private fun LeadingIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AppHeader() {
    val context = LocalContext.current
    val icon: ImageBitmap? = remember {
        runCatching {
            context.packageManager.getApplicationIcon(context.packageName)
                .toBitmap(width = 120, height = 120).asImageBitmap()
        }.getOrNull()
    }
    val label = remember {
        runCatching {
            context.packageManager.getApplicationLabel(context.applicationInfo).toString()
        }.getOrDefault(context.packageName)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape),
            )
            Spacer(Modifier.width(16.dp))
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = context.packageName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
