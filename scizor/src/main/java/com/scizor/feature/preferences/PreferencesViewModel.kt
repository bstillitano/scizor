package com.scizor.feature.preferences

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class PreferencesUiState(
    val files: List<String> = emptyList(),
    val selectedFile: String? = null,
    val entries: List<PrefEntry> = emptyList(),
)

internal class PreferencesViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(PreferencesUiState())
    val state: StateFlow<PreferencesUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val context = getApplication<Application>()
        val files = PreferencesBrowser.files(context)
        val selected = _state.value.selectedFile?.takeIf { it in files } ?: files.firstOrNull()
        _state.value = PreferencesUiState(
            files = files,
            selectedFile = selected,
            entries = selected?.let { PreferencesBrowser.entries(context, it) } ?: emptyList(),
        )
    }

    fun selectFile(file: String) {
        _state.value = _state.value.copy(
            selectedFile = file,
            entries = PreferencesBrowser.entries(getApplication(), file),
        )
    }

    fun stringSet(key: String): List<String> {
        val file = _state.value.selectedFile ?: return emptyList()
        return PreferencesBrowser.stringSet(getApplication(), file, key)
    }

    fun setString(key: String, value: String) = mutate { PreferencesBrowser.putString(getApplication(), it, key, value) }
    fun setBoolean(key: String, value: Boolean) = mutate { PreferencesBrowser.putBoolean(getApplication(), it, key, value) }
    fun setInt(key: String, value: Int) = mutate { PreferencesBrowser.putInt(getApplication(), it, key, value) }
    fun setLong(key: String, value: Long) = mutate { PreferencesBrowser.putLong(getApplication(), it, key, value) }
    fun setFloat(key: String, value: Float) = mutate { PreferencesBrowser.putFloat(getApplication(), it, key, value) }
    fun remove(key: String) = mutate { PreferencesBrowser.remove(getApplication(), it, key) }
    fun resetAll() = mutate { PreferencesBrowser.resetAll(getApplication(), it) }

    private inline fun mutate(block: (String) -> Unit) {
        val file = _state.value.selectedFile ?: return
        block(file)
        _state.value = _state.value.copy(entries = PreferencesBrowser.entries(getApplication(), file))
    }
}
