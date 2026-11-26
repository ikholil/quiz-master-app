package app.prostuti.app.di

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Mock sessions never expire. */
@Singleton
class SessionWatcher @Inject constructor() {
    val expired: Flow<Unit> = emptyFlow()
    fun start() = Unit
}
