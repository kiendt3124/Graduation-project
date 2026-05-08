package com.uet.expensetracker.core.platform

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uet.expensetracker.core.failure.Failure
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base interfaces for MVI pattern components
 */
interface MviStateBase

interface MviIntentBase

interface MviEventBase

/**
 * Base ViewModel class with MVI pattern implementation.
 * @see ViewModel
 * @see Failure
 */
abstract class BaseViewModel<S : MviStateBase, I : MviIntentBase, E : MviEventBase> : ViewModel() {
    private val _failure: MutableLiveData<Failure> = MutableLiveData()
    val failure: LiveData<Failure> = _failure

    protected abstract val _viewState: MutableStateFlow<S>
    val viewState: StateFlow<S> get() = _viewState.asStateFlow()

    private val _event = MutableSharedFlow<E?>()
    val event: SharedFlow<E?> = _event.asSharedFlow()

    abstract fun processIntent(intent: I)

    protected fun handleFailure(failure: Failure) {
        _failure.value = failure
    }

    protected fun emitEvent(event: E) {
        viewModelScope.launch {
            _event.emit(event)
        }
    }
}