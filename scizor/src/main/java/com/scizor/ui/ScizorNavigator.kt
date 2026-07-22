package com.scizor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf

/** A screen pushed onto the [ScizorNavigator] back stack. [id] is stable per entry. */
internal data class ScizorDestination(
    val id: Int,
    val title: String,
    val content: @Composable () -> Unit,
)

/**
 * Minimal in-activity navigation: the menu is the root, and feature screens are
 * pushed on top. Avoids a navigation-compose dependency for what is a shallow,
 * self-contained stack. Each destination carries a stable [ScizorDestination.id]
 * so the host can retain per-screen UI state (scroll position, etc.) across
 * push/pop via a SaveableStateHolder.
 */
internal class ScizorNavigator {

    val stack = mutableStateListOf<ScizorDestination>()
    private var nextId = 0

    val current: ScizorDestination?
        get() = stack.lastOrNull()

    fun push(title: String, content: @Composable () -> Unit) {
        stack.add(ScizorDestination(nextId++, title, content))
    }

    /** Pops the top screen. Returns false when already at the root (menu). */
    fun pop(): Boolean {
        if (stack.isEmpty()) return false
        stack.removeAt(stack.lastIndex)
        return true
    }
}
