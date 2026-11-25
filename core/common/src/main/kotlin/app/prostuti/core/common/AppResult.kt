package app.prostuti.core.common

/** Result type crossing the data → domain → UI boundary. Exceptions never leak past repositories. */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

/** Errors the UI knows how to explain. Each maps to a localized message in :core:ui. */
sealed interface AppError {
    data object NoConnection : AppError
    data object Timeout : AppError
    /** Refresh token rejected; the user must sign in again. */
    data object SessionExpired : AppError
    data class PremiumRequired(val feature: String?) : AppError
    data object NotFound : AppError
    data class Validation(val message: String, val fieldErrors: Map<String, String> = emptyMap()) : AppError
    data class RateLimited(val retryAfterSeconds: Long?) : AppError
    data class Conflict(val message: String) : AppError
    data class Server(val httpCode: Int, val message: String?) : AppError
    data class Unknown(val message: String?) : AppError
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <T, R> AppResult<T>.flatMap(transform: (T) -> AppResult<R>): AppResult<R> = when (this) {
    is AppResult.Success -> transform(data)
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(block: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) block(data)
    return this
}

inline fun <T> AppResult<T>.onFailure(block: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) block(error)
    return this
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data

fun <T> AppResult<T>.errorOrNull(): AppError? = (this as? AppResult.Failure)?.error

fun <T> T.asSuccess(): AppResult<T> = AppResult.Success(this)

val AppError.isRetryable: Boolean
    get() = this is AppError.NoConnection || this is AppError.Timeout ||
        (this is AppError.Server && httpCode >= 500) || this is AppError.RateLimited
