package app.prostuti.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.prostuti.app.di.SessionWatcher
import app.prostuti.core.domain.repository.AppSettings
import app.prostuti.core.domain.repository.AuthRepository
import app.prostuti.core.domain.repository.NetworkMonitor
import app.prostuti.core.domain.repository.SessionState
import app.prostuti.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class AppPhase { LOADING, SIGNED_OUT, ONBOARDING, MAIN }

@HiltViewModel
class MainViewModel @Inject constructor(
    auth: AuthRepository,
    settings: SettingsRepository,
    network: NetworkMonitor,
    sessionWatcher: SessionWatcher,
) : ViewModel() {
    init { sessionWatcher.start() }

    val session: StateFlow<SessionState> = auth.session.stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Unknown)
    val settings: StateFlow<AppSettings?> = settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val isOnline: StateFlow<Boolean> = network.isOnline.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
}

fun SessionState.phase(): AppPhase = when (this) {
    SessionState.Unknown -> AppPhase.LOADING
    SessionState.SignedOut -> AppPhase.SIGNED_OUT
    is SessionState.SignedIn -> if (onboardingCompleted) AppPhase.MAIN else AppPhase.ONBOARDING
}
