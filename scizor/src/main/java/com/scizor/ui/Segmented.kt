@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.scizor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Horizontal inset for segmented groups, matching the main menu. */
internal val SegmentInset = 16.dp

/**
 * Scizor's segmented-list colors: a clearly visible tonal fill. The M3 default
 * [ListItemDefaults.segmentedColors] container is near-invisible on Material You
 * dynamic schemes, so we raise it to `surfaceContainerHigh`.
 */
@Composable
internal fun scizorSegmentedColors(): ListItemColors =
    ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )

/**
 * Renders [items] as a Material 3 Expressive segmented group — rounded outer
 * ends, small inner joins, and gaps between segments. Best for short,
 * non-scrolling sections. For long lists inside a `LazyColumn`, call
 * [ListItemDefaults.segmentedShapes] directly with `itemsIndexed`.
 */
@Composable
internal fun <T> SegmentedColumn(
    items: List<T>,
    modifier: Modifier = Modifier,
    itemContent: @Composable (item: T, shapes: ListItemShapes) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SegmentInset),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        items.forEachIndexed { index, item ->
            itemContent(item, ListItemDefaults.segmentedShapes(index = index, count = items.size))
        }
    }
}

/** A grouped-list section header, styled like the menu subheaders. */
@Composable
internal fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 8.dp),
    )
}
