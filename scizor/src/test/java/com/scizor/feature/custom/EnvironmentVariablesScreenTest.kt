package com.scizor.feature.custom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.scizor.Scizor
import com.scizor.ui.ScizorTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Layout regression tests for the Environment Variables rows.
 *
 * A value used to be rendered in the list item's trailing slot, which is measured
 * before the headline and against the full row width — so a long value (a token, an
 * id) took the whole row and squeezed the key into a one-character-wide vertical
 * column. Robolectric's text metrics are not the device's, so these assert the
 * arrangement the row settles into rather than any particular width in dp.
 */
@RunWith(RobolectricTestRunner::class)
class EnvironmentVariablesScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        Scizor.start(RuntimeEnvironment.getApplication())
    }

    private fun show(vararg variables: Pair<String, String>) {
        Scizor.environmentVariables = variables.toMap()
        compose.setContent {
            ScizorTheme(darkTheme = false) {
                Box(modifier = Modifier.width(360.dp)) { EnvironmentVariablesScreen() }
            }
        }
    }

    @Test
    fun `value too wide to share the key's line is stacked under it`() {
        val key = "Firebase Token"
        val value = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9." + "a".repeat(300)
        show(key to value)

        val keyBounds = compose.onNodeWithText(key, useUnmergedTree = true).getBoundsInRoot()
        val valueBounds = compose.onNodeWithText(value, useUnmergedTree = true).getBoundsInRoot()

        assertTrue(
            "value should sit below the key, not beside it",
            valueBounds.top >= keyBounds.bottom,
        )
        assertEquals(
            "stacked key and value should share the row's left edge",
            keyBounds.left,
            valueBounds.left,
        )
        assertTrue(
            "stacked value should use the full row width",
            valueBounds.width > keyBounds.width,
        )
    }

    @Test
    fun `value that fits stays on the key's line`() {
        show("Env" to "staging")

        val keyBounds = compose.onNodeWithText("Env", useUnmergedTree = true).getBoundsInRoot()
        val valueBounds = compose.onNodeWithText("staging", useUnmergedTree = true).getBoundsInRoot()

        assertTrue(
            "value should sit to the right of the key",
            valueBounds.left >= keyBounds.right,
        )
        assertTrue(
            "value should share the key's line",
            valueBounds.top < keyBounds.bottom,
        )
    }
}
