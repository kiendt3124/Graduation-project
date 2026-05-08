package com.uet.expensetracker.utils

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Extension function to handle savedStateHandle properly
fun <T> SavedStateHandle.getStateFlow(key: String, initialValue: T): StateFlow<T> {
    val liveData = this.getLiveData(key, initialValue)
    val mutableStateFlow = MutableStateFlow(initialValue)

    // Create a StateFlow from the LiveData
    liveData.observeForever {
        mutableStateFlow.value = it
    }

    return mutableStateFlow
}