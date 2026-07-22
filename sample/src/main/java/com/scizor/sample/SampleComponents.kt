@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.scizor.sample

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Rows a [Section] can render as segmented-list items. */
internal sealed interface SampleRow {
    data class Action(
        val label: String,
        val enabled: Boolean = true,
        val destructive: Boolean = false,
        val loading: Boolean = false,
        val onClick: () -> Unit,
    ) : SampleRow

    data class Label(val label: String, val value: String, val valueColor: Color? = null) : SampleRow

    data class Info(val text: String) : SampleRow
}

/** The visible tonal fill for segmented rows, matching the Scizor menu. */
@Composable
internal fun sampleSegmentedColors() =
    ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)

/** A titled segmented-list group with an optional footer — the Material 3 equivalent of an iOS List section. */
@Composable
internal fun Section(title: String, footer: String? = null, rows: List<SampleRow>) {
    Column {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            rows.forEachIndexed { index, row ->
                RowItem(row, ListItemDefaults.segmentedShapes(index = index, count = rows.size))
            }
        }
        if (footer != null) {
            Text(
                footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp),
            )
        }
    }
}

@Composable
private fun RowItem(row: SampleRow, shapes: ListItemShapes) {
    when (row) {
        is SampleRow.Action -> SegmentedListItem(
            onClick = row.onClick,
            enabled = row.enabled,
            shapes = shapes,
            colors = sampleSegmentedColors(),
            trailingContent = if (row.loading) {
                { CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) }
            } else {
                null
            },
            content = {
                Text(
                    row.label,
                    color = if (row.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            },
        )
        is SampleRow.Label -> SegmentedListItem(
            shapes = shapes,
            colors = sampleSegmentedColors(),
            trailingContent = {
                Text(row.value, color = row.valueColor ?: MaterialTheme.colorScheme.onSurfaceVariant)
            },
            content = { Text(row.label) },
        )
        is SampleRow.Info -> SegmentedListItem(
            shapes = shapes,
            colors = sampleSegmentedColors(),
            content = {
                Text(row.text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            },
        )
    }
}

internal fun onMain(block: () -> Unit) {
    Handler(Looper.getMainLooper()).post(block)
}
