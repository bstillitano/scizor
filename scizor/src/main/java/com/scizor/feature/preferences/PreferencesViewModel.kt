package com.scizor.feature.preferences

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.scizor.Scizor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class PreferencesUiState(
    val files: List<String> = emptyList(),
    val selectedFile: String? = null,
    val entries: List<PrefEntry> = emptyList(),
    /** True when viewing Scizor's own settings store, which is read-only. */
    val readOnly: Boolean = false,
)

internal class PreferencesViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(PreferencesUiState())
    val state: StateFlow<PreferencesUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val hostFiles = PreferencesBrowser.files(getApplication())
        val files = hostFiles + if (scizorSnapshot().isNotEmpty()) listOf(SCIZOR_STORE) else emptyList()
        val selected = _state.value.selectedFile?.takeIf { it in files } ?: files.firstOrNull()
        _state.value = buildState(files, selected)
    }

    fun selectFile(file: String) {
        _state.value = buildState(_state.value.files, file)
    }

    private fun buildState(files: List<String>, selected: String?): PreferencesUiState {
        val readOnly = selected == SCIZOR_STORE
        val entries = when {
            selected == null -> emptyList()
            readOnly -> scizorEntries()
            else -> PreferencesBrowser.entries(getApplication(), selected)
        }
        return PreferencesUiState(files, selected, entries, readOnly)
    }

    fun stringSet(key: String): List<String> {
        val file = _state.value.selectedFile ?: return emptyList()
        if (file == SCIZOR_STORE) return emptyList()
        return PreferencesBrowser.stringSet(getApplication(), file, key)
    }

    fun setString(key: String, value: String) = mutate { PreferencesBrowser.putString(getApplication(), it, key, value) }
    fun setBoolean(key: String, value: Boolean) = mutate { PreferencesBrowser.putBoolean(getApplication(), it, key, value) }
    fun setInt(key: String, value: Int) = mutate { PreferencesBrowser.putInt(getApplication(), it, key, value) }
    fun setLong(key: String, value: Long) = mutate { PreferencesBrowser.putLong(getApplication(), it, key, value) }
    fun setFloat(key: String, value: Float) = mutate { PreferencesBrowser.putFloat(getApplication(), it, key, value) }
    fun setStringSet(key: String, values: Set<String>) =
        mutate { PreferencesBrowser.putStringSet(getApplication(), it, key, values) }
    fun remove(key: String) = mutate { PreferencesBrowser.remove(getApplication(), it, key) }
    fun resetAll() = mutate { PreferencesBrowser.resetAll(getApplication(), it) }

    private fun scizorSnapshot(): Map<String, Any> = Scizor.storeOrNull()?.snapshot() ?: emptyMap()

    private fun scizorEntries(): List<PrefEntry> = scizorSnapshot().map { (key, value) ->
        val (type, display) = when (value) {
            is Boolean -> "Boolean" to value.toString()
            is Int -> "Int" to value.toString()
            is Long -> "Long" to value.toString()
            is Float -> "Float" to value.toString()
            is String -> "String" to value
            is Set<*> -> "StringSet" to "${value.size} items"
            else -> (value.javaClass.simpleName ?: "Unknown") to value.toString()
        }
        PrefEntry(key, display, type)
    }.sortedBy { it.key }

    private inline fun mutate(block: (String) -> Unit) {
        val file = _state.value.selectedFile ?: return
        if (file == SCIZOR_STORE) return // read-only
        block(file)
        _state.value = _state.value.copy(entries = PreferencesBrowser.entries(getApplication(), file))
    }

    private companion object {
        const val SCIZOR_STORE = "Scizor settings"
    }
}
