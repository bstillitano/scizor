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
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Horizontal inset for segmented groups, matching the main menu. */
internal val SegmentInset = 16.dp

/**
 * Vertical gap between segments in a group.
 *
 * Delegates to Material 3's own value rather than hardcoding one, so the spacing
 * tracks the component it sits between.
 */
internal val ScizorSegmentedGap = ListItemDefaults.SegmentedGap

/**
 * Per-state shapes for one segment of a group.
 *
 * An alias for Material 3's [ListItemShapes] rather than a type of our own: the
 * segmented list is the real Expressive component, and the call sites should be
 * naming what they are actually passing.
 */
internal typealias ScizorListShapes = ListItemShapes

/**
 * Shapes for the segment at [index] within a group of [count] — large radii on the
 * group's outer ends, small on the joins between neighbours, and a corner morph
 * while the segment is pressed.
 */
@Composable
internal fun scizorSegmentedShapes(index: Int, count: Int): ScizorListShapes =
    ListItemDefaults.segmentedShapes(index = index, count = count)

/**
 * Scizor's segmented-list colors: a clearly visible tonal fill. The Material
 * default container is near-invisible on Material You dynamic schemes, so we
 * raise it to `surfaceContainerHigh`.
 */
@Composable
internal fun scizorSegmentedColors(
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
): ListItemColors = ListItemDefaults.segmentedColors(containerColor = containerColor)

/**
 * A single segment of a grouped list.
 *
 * A thin pass-through to Material 3 Expressive's [SegmentedListItem]. The
 * indirection is kept deliberately: it is the one place that has to change if the
 * Expressive API moves again, and it means the ~84 call sites across the menu
 * name a Scizor symbol rather than binding directly to a prerelease Material 3
 * signature.
 */
@Composable
internal fun ScizorListItem(
    shapes: ScizorListShapes,
    modifier: Modifier = Modifier,
    colors: ListItemColors = scizorSegmentedColors(),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    overlineContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    // SegmentedListItem's onClick is non-null, but a read-only row must not be
    // interactive at all — passing an empty lambda would give it a ripple and a
    // press morph for a tap that does nothing. So omit the click parameters
    // entirely when there is no handler, rather than faking one.
    if (onClick == null && onLongClick == null) {
        SegmentedListItem(
            shapes = shapes,
            modifier = modifier,
            colors = colors,
            leadingContent = leadingContent,
            overlineContent = overlineContent,
            supportingContent = supportingContent,
            trailingContent = trailingContent,
            content = content,
        )
    } else {
        SegmentedListItem(
            shapes = shapes,
            modifier = modifier,
            colors = colors,
            onClick = onClick ?: {},
            onLongClick = onLongClick,
            leadingContent = leadingContent,
            overlineContent = overlineContent,
            supportingContent = supportingContent,
            trailingContent = trailingContent,
            content = content,
        )
    }
}

/**
 * Renders [items] as a segmented group — rounded outer ends, small inner joins,
 * and gaps between segments. Best for short, non-scrolling sections. For long
 * lists inside a `LazyColumn`, call [scizorSegmentedShapes] directly with
 * `itemsIndexed`.
 */
@Composable
internal fun <T> SegmentedColumn(
    items: List<T>,
    modifier: Modifier = Modifier,
    itemContent: @Composable (item: T, shapes: ScizorListShapes) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SegmentInset),
        verticalArrangement = Arrangement.spacedBy(ScizorSegmentedGap),
    ) {
        items.forEachIndexed { index, item ->
            itemContent(item, scizorSegmentedShapes(index = index, count = items.size))
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
