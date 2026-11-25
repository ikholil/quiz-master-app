package app.prostuti.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.prostuti.core.common.AppError
import app.prostuti.core.common.AppResult
import app.prostuti.core.designsystem.component.ErrorState
import app.prostuti.core.designsystem.component.SkeletonList

@Immutable
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val error: AppError) : UiState<Nothing>
}

fun <T> AppResult<T>.toUiState(): UiState<T> = when (this) {
    is AppResult.Success -> UiState.Success(data)
    is AppResult.Failure -> UiState.Error(error)
}

val <T> UiState<T>.dataOrNull: T? get() = (this as? UiState.Success)?.data

/** Renders loading / error / content consistently on every screen. */
@Composable
fun <T> StateContent(
    state: UiState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    loading: @Composable () -> Unit = { SkeletonList(modifier) },
    content: @Composable (T) -> Unit,
) {
    when (state) {
        UiState.Loading -> loading()
        is UiState.Error -> ErrorView(state.error, onRetry, modifier)
        is UiState.Success -> content(state.data)
    }
}

@Composable
fun ErrorView(error: AppError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val offline = error is AppError.NoConnection
    ErrorState(
        icon = if (offline) Icons.Rounded.CloudOff else Icons.Rounded.ErrorOutline,
        title = stringResource(if (offline) R.string.ui_error_offline_title else R.string.ui_error_title),
        message = errorMessage(error),
        retryLabel = stringResource(R.string.ui_retry),
        onRetry = onRetry,
        modifier = modifier,
    )
}

@Composable
fun errorMessage(error: AppError): String = when (error) {
    AppError.NoConnection -> stringResource(R.string.ui_error_offline)
    AppError.Timeout -> stringResource(R.string.ui_error_timeout)
    AppError.SessionExpired -> stringResource(R.string.ui_error_session)
    is AppError.PremiumRequired -> stringResource(R.string.ui_error_premium)
    AppError.NotFound -> stringResource(R.string.ui_error_not_found)
    is AppError.Validation -> error.message
    is AppError.RateLimited -> stringResource(R.string.ui_error_rate_limited)
    is AppError.Conflict -> error.message
    is AppError.Server -> error.message ?: stringResource(R.string.ui_error_server, error.httpCode)
    is AppError.Unknown -> stringResource(R.string.ui_error_unknown)
}
