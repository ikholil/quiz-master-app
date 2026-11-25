package app.prostuti.core.network.auth

import app.prostuti.core.datastore.TokenStore
import app.prostuti.core.network.api.RefreshApi
import app.prostuti.core.network.dto.RefreshRequestDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Broadcasts that the refresh token is no longer valid so the app can return to sign-in. */
@Singleton
class SessionEvents @Inject constructor() {
    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val expired: SharedFlow<Unit> = _expired.asSharedFlow()
    fun notifyExpired() { _expired.tryEmit(Unit) }
}

/**
 * Serializes refreshes: when ten requests fail with 401 at once, only the first refreshes and the
 * others reuse the new token. A rejected refresh token clears the session.
 */
@Singleton
class TokenRefresher @Inject constructor(
    private val tokenStore: TokenStore,
    private val refreshApi: RefreshApi,
    private val sessionEvents: SessionEvents,
) {
    private val mutex = Mutex()

    sealed interface Outcome {
        data class Refreshed(val accessToken: String) : Outcome
        data object Rejected : Outcome
        data object Failed : Outcome
    }

    suspend fun refresh(failedAccessToken: String?): Outcome = mutex.withLock {
        val current = tokenStore.read() ?: return Outcome.Rejected
        // Another caller already refreshed while we waited for the lock.
        if (failedAccessToken != null && current.accessToken != failedAccessToken) {
            return Outcome.Refreshed(current.accessToken)
        }
        try {
            val dto = refreshApi.refresh(RefreshRequestDto(current.refreshToken)).data
            tokenStore.write(
                TokenStore.Tokens(dto.accessToken, dto.refreshToken, Instant.parse(dto.accessExpiresAt)),
            )
            Outcome.Refreshed(dto.accessToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                tokenStore.clear()
                sessionEvents.notifyExpired()
                Outcome.Rejected
            } else Outcome.Failed
        } catch (e: Exception) {
            Outcome.Failed
        }
    }
}
