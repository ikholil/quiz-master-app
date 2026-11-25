package app.prostuti.core.network

import app.prostuti.core.common.AppError
import app.prostuti.core.common.AppResult
import app.prostuti.core.network.dto.ErrorBodyDto
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Centralized error handling: every repository call goes through here. */
suspend inline fun <T> safeApiCall(json: Json = NetworkJson, crossinline call: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(call())
    } catch (e: CancellationException) {
        throw e // Never swallow cancellation; the screen went away.
    } catch (e: Throwable) {
        AppResult.Failure(e.toAppError(json))
    }

fun Throwable.toAppError(json: Json = NetworkJson): AppError = when (this) {
    is SessionExpiredException -> AppError.SessionExpired
    is SocketTimeoutException, is InterruptedIOException -> AppError.Timeout
    is UnknownHostException, is NoConnectivityException -> AppError.NoConnection
    is HttpException -> httpError(this, json)
    is IOException -> AppError.NoConnection
    is kotlinx.serialization.SerializationException -> AppError.Unknown("Unexpected response from server")
    else -> AppError.Unknown(message)
}

private fun httpError(e: HttpException, json: Json): AppError {
    val body = runCatching {
        e.response()?.errorBody()?.string()?.let { json.decodeFromString(ErrorBodyDto.serializer(), it) }
    }.getOrNull()
    val message = body?.message
    return when (e.code()) {
        401 -> AppError.SessionExpired
        402, 403 -> if (body?.code == "PREMIUM_REQUIRED" || e.code() == 402) AppError.PremiumRequired(body?.entitlement)
        else AppError.Server(e.code(), message)
        404 -> AppError.NotFound
        409 -> AppError.Conflict(message ?: "Conflict")
        400, 422 -> AppError.Validation(message ?: "Invalid input", body?.fieldErrors.orEmpty())
        429 -> AppError.RateLimited(e.response()?.headers()?.get("Retry-After")?.toLongOrNull())
        else -> AppError.Server(e.code(), message)
    }
}

class SessionExpiredException : IOException("Session expired")
class NoConnectivityException : IOException("No network connection")

val NetworkJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    isLenient = true
}
