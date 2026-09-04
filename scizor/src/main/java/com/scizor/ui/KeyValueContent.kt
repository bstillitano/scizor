package com.scizor.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A key on the left and its value on the right, stacking the value under the key
 * when the two cannot share a line.
 *
 * Belongs in a list item's `content` slot, with the trailing slot left empty. The
 * obvious alternative — key in `content`, value in `trailingContent` — is what this
 * replaces: a list item measures its trailing slot first and against the full row
 * width, so a long value (a token, an id, a URL) takes the whole row and squeezes
 * the key into a one-character-wide vertical column.
 *
 * The choice between the two arrangements is measured rather than guessed at from
 * string length: both texts are asked for the width they want on a single line, and
 * they sit side by side only if those widths plus [horizontalGap] fit.
 */
@Composable
internal fun KeyValueContent(
    key: String,
    value: String,
    modifier: Modifier = Modifier,
    horizontalGap: Dp = 16.dp,
    verticalGap: Dp = 2.dp,
) {
    Layout(
        modifier = modifier,
        content = {
            Text(key)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    ) { measurables, constraints ->
        val (keyMeasurable, valueMeasurable) = measurables
        // Height is never the binding dimension here: the row grows to fit whichever
        // arrangement is chosen, so measure children free of the incoming height.
        val available = constraints.maxWidth
        val keyWidth = keyMeasurable.maxIntrinsicWidth(Constraints.Infinity)
        val valueWidth = valueMeasurable.maxIntrinsicWidth(Constraints.Infinity)
        val gap = horizontalGap.roundToPx()

        if (constraints.hasBoundedWidth && keyWidth + gap + valueWidth <= available) {
            val keyPlaceable = keyMeasurable.measure(Constraints(maxWidth = keyWidth))
            val valuePlaceable = valueMeasurable.measure(Constraints(maxWidth = valueWidth))
            val height = maxOf(keyPlaceable.height, valuePlaceable.height)
            layout(available, height) {
                keyPlaceable.place(x = 0, y = (height - keyPlaceable.height) / 2)
                valuePlaceable.place(
                    x = available - valuePlaceable.width,
                    y = (height - valuePlaceable.height) / 2,
                )
            }
        } else {
            val stacked = Constraints(maxWidth = available)
            val keyPlaceable = keyMeasurable.measure(stacked)
            val valuePlaceable = valueMeasurable.measure(stacked)
            val spacing = verticalGap.roundToPx()
            layout(
                width = maxOf(keyPlaceable.width, valuePlaceable.width),
                height = keyPlaceable.height + spacing + valuePlaceable.height,
            ) {
                keyPlaceable.place(x = 0, y = 0)
                valuePlaceable.place(x = 0, y = keyPlaceable.height + spacing)
            }
        }
    }
}
