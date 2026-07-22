package com.scizor.core

import com.scizor.Scizor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks pinned menu-entry ids, oldest-first, persisted via the Scizor store.
 * Mirrors Scyther's menu pinning.
 */
internal object MenuPins {

    private const val KEY = "menu_pins"

    private val _pins = MutableStateFlow<List<String>>(emptyList())
    val pins: StateFlow<List<String>> = _pins.asStateFlow()

    fun init() {
        val stored = Scizor.storeOrNull()?.string(KEY).orEmpty()
        _pins.value = stored.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun isPinned(id: String): Boolean = id in _pins.value

    fun toggle(id: String) {
        val current = _pins.value
        val updated = if (id in current) current - id else current + id
        _pins.value = updated
        Scizor.storeOrNull()?.putString(KEY, updated.joinToString(","))
    }
}
