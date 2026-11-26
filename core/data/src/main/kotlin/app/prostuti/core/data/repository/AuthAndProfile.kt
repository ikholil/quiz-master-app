package app.prostuti.core.data.repository

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import app.prostuti.core.common.AppResult
import app.prostuti.core.common.map
import app.prostuti.core.common.onSuccess
import app.prostuti.core.data.mapper.toDto
import app.prostuti.core.data.mapper.toModel
import app.prostuti.core.datastore.TokenStore
import app.prostuti.core.datastore.UserPreferencesDataSource
import app.prostuti.core.domain.repository.*
import app.prostuti.core.model.*
import app.prostuti.core.network.api.AuthApi
import app.prostuti.core.network.api.ProfileApi
import app.prostuti.core.network.auth.SessionEvents
import app.prostuti.core.network.dto.*
import app.prostuti.core.network.safeApiCall
import kotlinx.coroutines.flow.*
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveAuthRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenStore: TokenStore,
    private val prefs: UserPreferencesDataSource,
    private val sessionEvents: SessionEvents,
) : AuthRepository {

    override val session: Flow<SessionState> = prefs.preferences
        .map { p -> if (p.userId != null && tokenStore.read() != null) SessionState.SignedIn(p.userId!!, p.onboardingCompleted) else SessionState.SignedOut }
        .onStart { emit(SessionState.Unknown) }
        .distinctUntilChanged()

    override suspend fun requestOtp(phone: String) = safeApiCall { api.requestOtp(OtpRequestDto(phone)).data.toModel() }

    override suspend fun verifyOtp(challengeId: String, code: String) =
        safeApiCall { api.verifyOtp(OtpVerifyDto(challengeId, code)).data }.persist()

    override suspend fun signInWithEmail(email: String, password: String) =
        safeApiCall { api.emailLogin(EmailLoginDto(email.trim(), password)).data }.persist()

    override suspend fun registerWithEmail(name: String, email: String, password: String) =
        safeApiCall { api.emailRegister(EmailRegisterDto(name.trim(), email.trim(), password)).data }.persist()

    override suspend fun requestPasswordReset(email: String) =
        safeApiCall { api.forgotPassword(ForgotPasswordDto(email.trim())); Unit }

    override suspend fun signOut() {
        tokenStore.read()?.let { tokens -> safeApiCall { api.logout(RefreshRequestDto(tokens.refreshToken)) } }
        tokenStore.clear()
        prefs.setSession(null, false)
    }

    private suspend fun AppResult<AuthResponseDto>.persist(): AppResult<AuthSession> {
        onSuccess { dto ->
            tokenStore.write(TokenStore.Tokens(dto.tokens.accessToken, dto.tokens.refreshToken, Instant.parse(dto.tokens.accessExpiresAt)))
            prefs.setSession(dto.user.id, dto.user.onboardingCompleted)
        }
        return map { AuthSession(it.user.id, it.isNewUser) }
    }

    /** Called by the app when the refresh token is rejected. */
    suspend fun onSessionExpired() {
        tokenStore.clear()
        prefs.setSession(null, false)
    }

    val expiredEvents get() = sessionEvents.expired
}

@Singleton
class LiveProfileRepository @Inject constructor(
    private val api: ProfileApi,
    private val prefs: UserPreferencesDataSource,
) : ProfileRepository {
    private val cached = MutableStateFlow<StudentProfile?>(null)
    override val profile: Flow<StudentProfile?> = cached

    override suspend fun refresh() = safeApiCall { api.profile().data.toModel() }.onSuccess { cached.value = it }

    override suspend fun savePreferences(preferences: PreparationPreferences) =
        safeApiCall { api.savePreferences(preferences.toDto()).data.toModel() }.onSuccess { cached.value = it }

    override suspend fun previewInitialPlan(preferences: PreparationPreferences) =
        safeApiCall { api.previewPlan(preferences.toDto()).data.map { it.toModel() } }

    override suspend fun completeOnboarding(): AppResult<Unit> =
        safeApiCall { api.completeOnboarding(); Unit }.onSuccess {
            val userId = prefs.preferences.first().userId
            prefs.setSession(userId, onboardingCompleted = true)
            cached.value = cached.value?.copy(onboardingCompleted = true)
        }
}

@Singleton
class LiveSettingsRepository @Inject constructor(
    private val prefs: UserPreferencesDataSource,
    private val reminders: app.prostuti.core.data.sync.ReminderScheduler,
) : SettingsRepository {
    override val settings: Flow<AppSettings> = prefs.preferences.map { p ->
        AppSettings(
            language = if (p.languageTag == "en") AppLanguage.ENGLISH else AppLanguage.BENGALI,
            theme = runCatching { ThemePreference.valueOf(p.theme) }.getOrDefault(ThemePreference.SYSTEM),
            dataSaver = p.dataSaver,
            downloadOnWifiOnly = p.downloadOnWifiOnly,
            studyReminderHour = p.studyReminderHour,
        )
    }.distinctUntilChanged()

    override suspend fun setLanguage(language: AppLanguage) {
        prefs.setLanguage(language.tag)
        // Per-app language: persisted by AppCompat and applied to all resources, including on API < 33.
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }

    override suspend fun setTheme(theme: ThemePreference) { prefs.setTheme(theme.name) }
    override suspend fun setDataSaver(enabled: Boolean) { prefs.setDataSaver(enabled) }
    override suspend fun setDownloadOnWifiOnly(enabled: Boolean) { prefs.setDownloadOnWifiOnly(enabled) }
    override suspend fun setStudyReminderHour(hour: Int?) {
        prefs.setStudyReminderHour(hour)
        reminders.schedule(hour)
    }
}
