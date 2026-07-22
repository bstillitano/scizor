package com.scizor.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.scizor.ui.ScizorNavigator

/**
 * A single entry in the Scizor debug menu, contributed by a feature module.
 *
 * @param id stable identifier, used to de-duplicate re-registration.
 * @param section group header the entry is listed under.
 * @param screen the Composable shown when the entry is tapped. Receives the
 *   [ScizorNavigator] so a feature can push its own child pages.
 */
internal data class ScizorMenuEntry(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val section: String,
    val screen: @Composable (ScizorNavigator) -> Unit,
)

/**
 * In-memory registry of the built-in feature screens. Features register
 * themselves during [com.scizor.Scizor.start]; the menu reads the result.
 */
internal object FeatureRegistry {

    private val entries = mutableListOf<ScizorMenuEntry>()

    fun register(entry: ScizorMenuEntry) {
        entries.removeAll { it.id == entry.id }
        entries.add(entry)
    }

    fun all(): List<ScizorMenuEntry> = entries.toList()

    fun clear() = entries.clear()
}
