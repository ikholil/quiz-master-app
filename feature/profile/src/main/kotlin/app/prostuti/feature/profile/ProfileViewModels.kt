package app.prostuti.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.prostuti.core.common.AppError
import app.prostuti.core.common.AppResult
import app.prostuti.core.common.getOrNull
import app.prostuti.core.domain.repository.*
import app.prostuti.core.model.*
import app.prostuti.core.ui.LoadViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val account: AccountRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    val profile = profiles.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val subscription = account.subscription.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubscriptionStatus.Free)
    val achievements = MutableStateFlow<List<Achievement>>(emptyList())

    init {
        viewModelScope.launch {
            profiles.refresh()
            account.refreshSubscription()
            achievements.value = account.achievements().getOrNull().orEmpty()
        }
    }

    fun signOut() = viewModelScope.launch { auth.signOut() }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    val state = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun setLanguage(l: AppLanguage) = viewModelScope.launch { settings.setLanguage(l) }
    fun setTheme(t: ThemePreference) = viewModelScope.launch { settings.setTheme(t) }
    fun setReminder(h: Int?) = viewModelScope.launch { settings.setStudyReminderHour(h) }
}

@HiltViewModel
class PrivacyViewModel @Inject constructor(private val account: AccountRepository) : LoadViewModel<PrivacySettings>() {
    init { load() }
    override suspend fun fetch() = account.privacy()

    fun change(transform: (PrivacySettings) -> PrivacySettings) {
        var next: PrivacySettings? = null
        update { transform(it).also { n -> next = n } }
        next?.let { n -> launchAction { account.updatePrivacy(n) } }
    }
}

@HiltViewModel
class SubscriptionViewModel @Inject constructor(private val account: AccountRepository) : LoadViewModel<List<SubscriptionPlan>>() {
    val status = account.subscription.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubscriptionStatus.Free)
    val checkingOut = MutableStateFlow(false)
    init { load() }
    override suspend fun fetch() = account.plans()

    /** The backend creates the payment session; the app never handles card or wallet credentials. */
    fun checkout(planId: String, open: (String) -> Unit) = viewModelScope.launch {
        checkingOut.value = true
        when (val r = account.checkout(planId)) {
            is AppResult.Success -> open(r.data)
            is AppResult.Failure -> report(r.error)
        }
        checkingOut.value = false
    }
}

@HiltViewModel
class SupportViewModel @Inject constructor(private val account: AccountRepository) : LoadViewModel<List<SupportTicket>>() {
    init { load() }
    override suspend fun fetch() = account.tickets()

    fun create(subject: String, message: String, onDone: () -> Unit) = viewModelScope.launch {
        when (val r = account.createTicket(subject, message)) {
            is AppResult.Success -> { update { listOf(r.data) + it }; onDone() }
            is AppResult.Failure -> report(r.error)
        }
    }
}

@HiltViewModel
class AccountDataViewModel @Inject constructor(
    private val account: AccountRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<AppError?>(null)

    fun export(onDone: () -> Unit) = run { account.requestDataExport() }.let { it(onDone) }
    fun delete(reason: String, onDone: () -> Unit) = run { account.requestAccountDeletion(reason).also { r -> if (r is AppResult.Success) auth.signOut() } }.let { it(onDone) }

    private fun run(block: suspend () -> AppResult<*>): (() -> Unit) -> Unit = { onDone ->
        viewModelScope.launch {
            busy.value = true
            when (val r = block()) {
                is AppResult.Success -> onDone()
                is AppResult.Failure -> error.value = r.error
            }
            busy.value = false
        }
    }
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(private val account: AccountRepository) : LoadViewModel<List<AppNotification>>() {
    init { load() }
    override suspend fun fetch() = account.notifications()
}
