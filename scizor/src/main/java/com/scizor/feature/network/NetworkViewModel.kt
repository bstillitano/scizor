package com.scizor.feature.network

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

internal class NetworkViewModel : ViewModel() {

    val transactions: StateFlow<List<NetworkTransaction>> = NetworkLogger.transactions

    fun clear() = NetworkLogger.clear()

    fun find(id: Long): NetworkTransaction? = NetworkLogger.find(id)
}
