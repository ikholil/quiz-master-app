package app.prostuti.app.di

import app.prostuti.core.common.ApplicationScope
import app.prostuti.core.data.repository.LiveAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Signs the user out locally when the backend rejects the refresh token. */
@Singleton
class SessionWatcher @Inject constructor(
    private val auth: LiveAuthRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    val expired: Flow<Unit> = auth.expiredEvents.map { auth.onSessionExpired() }

    fun start() = scope.launch { expired.collect { } }
}
