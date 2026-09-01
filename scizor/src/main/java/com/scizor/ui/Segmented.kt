package com.scizor.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Horizontal inset for segmented groups, matching the main menu. */
internal val SegmentInset = 16.dp

/** Vertical gap between segments in a group. */
internal val ScizorSegmentedGap = 2.dp

/** Radius on a group's outer ends. */
private val OuterRadius = 20.dp

/** Radius on the joins between adjacent segments. */
private val InnerRadius = 4.dp

/** Radius every corner animates toward while a segment is pressed. */
private val PressedRadius = 12.dp

/**
 * Per-corner radii for one segment of a group.
 *
 * Held as four [Dp] values rather than a `Shape` so each corner can be animated
 * independently for the press-time morph in [ScizorListItem].
 */
@Immutable
internal data class ScizorListShapes(
    val topStart: Dp,
    val topEnd: Dp,
    val bottomStart: Dp,
    val bottomEnd: Dp,
)

/**
 * Radii for the segment at [index] within a group of [count]: large on the
 * group's outer ends, small on the joins between neighbours.
 *
 * A single-item group (`count == 1`) is fully rounded, which falls out of the
 * two conditions being simultaneously true.
 */
internal fun scizorSegmentedShapes(index: Int, count: Int): ScizorListShapes {
    val top = if (index == 0) OuterRadius else InnerRadius
    val bottom = if (index == count - 1) OuterRadius else InnerRadius
    return ScizorListShapes(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/**
 * Scizor's segmented-list colors: a clearly visible tonal fill. The Material
 * default container is near-invisible on Material You dynamic schemes, so we
 * raise it to `surfaceContainerHigh`.
 */
@Composable
internal fun scizorSegmentedColors(
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
): ListItemColors = ListItemDefaults.colors(containerColor = containerColor)

/**
 * A single segment of a grouped list.
 *
 * Built on the stable Material 3 [ListItem] rather than the Expressive
 * `SegmentedListItem`, so Scizor does not drag consumers onto a prerelease
 * Material 3. Reproduces the segmented look with [Modifier.clip] and animates
 * the corner radii on press.
 *
 * When Material 3 1.5.0 ships stable, the body of this function can revert to
 * `SegmentedListItem` and no call site needs to change.
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
    val interaction = remember { MutableInteractionSource() }
    val interactive = onClick != null || onLongClick != null
    val pressed by interaction.collectIsPressedAsState()
    val morphing = interactive && pressed

    val topStart by animateDpAsState(
        if (morphing) PressedRadius else shapes.topStart, label = "segment-top-start",
    )
    val topEnd by animateDpAsState(
        if (morphing) PressedRadius else shapes.topEnd, label = "segment-top-end",
    )
    val bottomStart by animateDpAsState(
        if (morphing) PressedRadius else shapes.bottomStart, label = "segment-bottom-start",
    )
    val bottomEnd by animateDpAsState(
        if (morphing) PressedRadius else shapes.bottomEnd, label = "segment-bottom-end",
    )

    // RoundedCornerShape's Dp overload takes corners clockwise from the top-start.
    val shape = RoundedCornerShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart,
    )

    // Clip before combinedClickable so the ripple is bounded by the rounded shape.
    val clickable = if (interactive) {
        Modifier.combinedClickable(
            interactionSource = interaction,
            indication = ripple(),
            onClick = onClick ?: {},
            onLongClick = onLongClick,
        )
    } else {
        Modifier
    }

    ListItem(
        headlineContent = content,
        modifier = modifier.clip(shape).then(clickable),
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        colors = colors,
    )
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
