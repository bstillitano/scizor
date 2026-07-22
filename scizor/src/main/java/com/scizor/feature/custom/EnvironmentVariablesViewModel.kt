package com.scizor.feature.custom

import androidx.lifecycle.ViewModel
import com.scizor.Scizor

internal class EnvironmentVariablesViewModel : ViewModel() {
    fun variables(): List<Pair<String, String>> =
        Scizor.environmentVariables.entries
            .sortedBy { it.key }
            .map { it.key to it.value }
}
