package com.scizor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf

/** A screen pushed onto the [ScizorNavigator] back stack. */
internal data class ScizorDestination(
    val title: String,
    val content: @Composable () -> Unit,
)

/**
 * Minimal in-activity navigation: the menu is the root, and feature screens are
 * pushed on top. Avoids a navigation-compose dependency for what is a shallow,
 * self-contained stack.
 */
internal class ScizorNavigator {

    val stack = mutableStateListOf<ScizorDestination>()

    val current: ScizorDestination?
        get() = stack.lastOrNull()

    fun push(title: String, content: @Composable () -> Unit) {
        stack.add(ScizorDestination(title, content))
    }

    /** Pops the top screen. Returns false when already at the root (menu). */
    fun pop(): Boolean {
        if (stack.isEmpty()) return false
        stack.removeAt(stack.lastIndex)
        return true
    }
}
