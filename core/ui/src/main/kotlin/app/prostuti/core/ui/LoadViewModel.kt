package app.prostuti.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.prostuti.core.common.AppError
import app.prostuti.core.common.AppResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Base for screens that load one thing. Re-loading cancels the previous request (request
 * cancellation), and keeps showing stale data while refreshing instead of flashing a skeleton.
 */
abstract class LoadViewModel<T> : ViewModel() {
    private val _state = MutableStateFlow<UiState<T>>(UiState.Loading)
    val state: StateFlow<UiState<T>> = _state.asStateFlow()

    private val _messages = Channel<AppError>(Channel.BUFFERED)
    /** One-off errors for snackbars (e.g. a failed bookmark), separate from the screen state. */
    val messages = _messages.receiveAsFlow()

    private var loadJob: Job? = null

    protected abstract suspend fun fetch(): AppResult<T>

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (_state.value !is UiState.Success) _state.value = UiState.Loading
            val result = fetch()
            _state.value = when {
                result is AppResult.Failure && _state.value is UiState.Success -> { _messages.send(result.error); _state.value }
                else -> result.toUiState()
            }
        }
    }

    protected fun update(transform: (T) -> T) {
        val current = _state.value
        if (current is UiState.Success) _state.value = UiState.Success(transform(current.data))
    }

    protected suspend fun report(error: AppError) = _messages.send(error)

    protected fun launchAction(block: suspend () -> AppResult<*>) {
        viewModelScope.launch {
            val r = block()
            if (r is AppResult.Failure) _messages.send(r.error)
        }
    }
}
