package com.scizor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Backs the single search field hosted in the debug menu's top app bar. The
 * current screen registers a placeholder via [rememberSearchQuery]; the app bar
 * then shows a magnifier that expands the title into a plain text field. State is
 * owned by the host so search lives in the header, not inside each screen.
 */
internal class ScizorSearchController {
    /** The screen instance that currently owns search (null when none). */
    var owner: Any? by mutableStateOf(null)

    /** Placeholder for the active screen, or null when the screen has no search. */
    var placeholder: String? by mutableStateOf(null)

    /** Whether the app-bar search field is expanded. */
    var active: Boolean by mutableStateOf(false)

    /** The live query text. */
    var query: String by mutableStateOf("")

    fun collapse() {
        active = false
        query = ""
    }
}

internal val LocalScizorSearch = staticCompositionLocalOf<ScizorSearchController?> { null }

/**
 * Declares that the calling screen supports search and returns the live query to
 * filter by. The field itself is rendered by the host in the top app bar. Returns
 * an empty string on screens reached without a search host.
 */
@Composable
internal fun rememberSearchQuery(placeholder: String): String {
    val controller = LocalScizorSearch.current ?: return ""
    val token = remember { Any() }
    DisposableEffect(controller, placeholder) {
        controller.owner = token
        controller.placeholder = placeholder
        controller.active = false
        controller.query = ""
        onDispose {
            // Only clear if we still own it — during a push the incoming screen has
            // already taken ownership, so the outgoing screen must not wipe it.
            if (controller.owner === token) {
                controller.owner = null
                controller.placeholder = null
                controller.active = false
                controller.query = ""
            }
        }
    }
    return if (controller.owner === token) controller.query else ""
}
