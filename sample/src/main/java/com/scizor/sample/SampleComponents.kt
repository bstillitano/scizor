package com.scizor.sample

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
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
internal fun sampleSegmentedColors(): ListItemColors =
    ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)

/** Radius on a group's outer ends. */
private val OuterRadius = 20.dp

/** Radius on the joins between adjacent segments. */
private val InnerRadius = 4.dp

/** Radius every corner animates toward while a segment is pressed. */
private val PressedRadius = 12.dp

/** Gap between segments in a group. */
private val SegmentedGap = 2.dp

/** Alpha applied to a disabled row's content, matching the standard disabled reading. */
private const val DisabledContentAlpha = 0.38f

/**
 * Per-corner radii for one segment of a group.
 *
 * Held as four [Dp] values rather than a `Shape` so each corner can be animated
 * independently for the press-time morph in [SampleListItem].
 */
@Immutable
private data class SampleListShapes(
    val topStart: Dp,
    val topEnd: Dp,
    val bottomStart: Dp,
    val bottomEnd: Dp,
)

/**
 * Radii for the segment at [index] within a group of [count]: large on the
 * group's outer ends, small on the joins between neighbours.
 */
private fun sampleSegmentedShapes(index: Int, count: Int): SampleListShapes {
    val top = if (index == 0) OuterRadius else InnerRadius
    val bottom = if (index == count - 1) OuterRadius else InnerRadius
    return SampleListShapes(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

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
        Column(verticalArrangement = Arrangement.spacedBy(SegmentedGap)) {
            rows.forEachIndexed { index, row ->
                RowItem(row, sampleSegmentedShapes(index = index, count = rows.size))
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

/**
 * A single segment of a grouped list, local to the sample.
 *
 * Built on the stable Material 3 [ListItem] rather than the Expressive
 * segmented list item, so the sample does not depend on a prerelease Material 3
 * API — the sample can't reuse Scizor's internal `ScizorListItem` since that
 * type isn't visible outside the `:scizor` module. Reproduces the segmented
 * look with [Modifier.clip] and animates the corner radii on press, and adds
 * an [enabled] flag the Expressive component had but stable `ListItem` doesn't.
 */
@Composable
private fun SampleListItem(
    shapes: SampleListShapes,
    modifier: Modifier = Modifier,
    colors: ListItemColors = sampleSegmentedColors(),
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val interactive = enabled && onClick != null
    val pressed by interaction.collectIsPressedAsState()
    val morphing = interactive && pressed

    val topStart by animateDpAsState(if (morphing) PressedRadius else shapes.topStart, label = "segment-top-start")
    val topEnd by animateDpAsState(if (morphing) PressedRadius else shapes.topEnd, label = "segment-top-end")
    val bottomStart by animateDpAsState(if (morphing) PressedRadius else shapes.bottomStart, label = "segment-bottom-start")
    val bottomEnd by animateDpAsState(if (morphing) PressedRadius else shapes.bottomEnd, label = "segment-bottom-end")

    // RoundedCornerShape's Dp overload takes corners clockwise from the top-start.
    val shape = RoundedCornerShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart,
    )

    // Clip before combinedClickable so the ripple is bounded by the rounded shape.
    val clickable = if (onClick != null) {
        Modifier.combinedClickable(
            interactionSource = interaction,
            indication = ripple(),
            enabled = enabled,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    val contentAlpha = if (enabled) 1f else DisabledContentAlpha

    ListItem(
        headlineContent = content,
        modifier = modifier.clip(shape).then(clickable).alpha(contentAlpha),
        trailingContent = trailingContent,
        colors = colors,
    )
}

@Composable
private fun RowItem(row: SampleRow, shapes: SampleListShapes) {
    when (row) {
        is SampleRow.Action -> SampleListItem(
            shapes = shapes,
            colors = sampleSegmentedColors(),
            enabled = row.enabled,
            onClick = row.onClick,
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
        is SampleRow.Label -> SampleListItem(
            shapes = shapes,
            colors = sampleSegmentedColors(),
            trailingContent = {
                Text(row.value, color = row.valueColor ?: MaterialTheme.colorScheme.onSurfaceVariant)
            },
            content = { Text(row.label) },
        )
        is SampleRow.Info -> SampleListItem(
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
