package com.scizor.feature.deviceinfo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class DeviceInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val _rows = MutableStateFlow<List<InfoRow>>(emptyList())
    val rows: StateFlow<List<InfoRow>> = _rows.asStateFlow()

    init {
        _rows.value = DeviceInfo.collect(application)
    }
}
