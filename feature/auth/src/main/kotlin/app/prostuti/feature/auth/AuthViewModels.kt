package app.prostuti.feature.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.prostuti.core.common.AppError
import app.prostuti.core.common.AppResult
import app.prostuti.core.domain.repository.AuthRepository
import app.prostuti.core.domain.repository.SettingsRepository
import app.prostuti.core.model.AppLanguage
import app.prostuti.core.model.OtpChallenge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** Bangladeshi mobile numbers: 01[3-9] followed by 8 digits; +88/88 prefix optional. */
object PhoneNumbers {
    private val pattern = Regex("^01[3-9]\\d{8}$")

    fun normalize(input: String): String {
        val digits = input.map { c -> if (c in '০'..'৯') '0' + (c - '০') else c }.filter { it.isDigit() }.joinToString("")
        return when {
            digits.startsWith("880") -> digits.removePrefix("88")
            digits.startsWith("88") && digits.length == 13 -> digits.removePrefix("88")
            else -> digits
        }
    }

    fun isValid(input: String) = pattern.matches(normalize(input))
}

@HiltViewModel
class WelcomeViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    val language: StateFlow<AppLanguage> = settings.settings.map { it.language }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppLanguage.BENGALI)

    fun setLanguage(language: AppLanguage) = viewModelScope.launch { settings.setLanguage(language) }
}

data class PhoneUiState(val phone: String = "", val loading: Boolean = false, val error: AppError? = null, val showValidation: Boolean = false) {
    val isValid get() = PhoneNumbers.isValid(phone)
}

@HiltViewModel
class PhoneViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(PhoneUiState())
    val state = _state.asStateFlow()
    private val _sent = Channel<OtpChallenge>(Channel.BUFFERED)
    val sent = _sent.receiveAsFlow()

    fun onPhoneChange(value: String) = _state.update { it.copy(phone = value.take(14), error = null) }

    fun submit() {
        val s = _state.value
        if (!s.isValid) { _state.update { it.copy(showValidation = true) }; return }
        if (s.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val r = auth.requestOtp(PhoneNumbers.normalize(s.phone))) {
                is AppResult.Success -> { _state.update { it.copy(loading = false) }; _sent.send(r.data) }
                is AppResult.Failure -> _state.update { it.copy(loading = false, error = r.error) }
            }
        }
    }
}

data class OtpUiState(
    val phone: String,
    val codeLength: Int,
    val code: String = "",
    val secondsToResend: Int = 0,
    val verifying: Boolean = false,
    val error: AppError? = null,
)

@HiltViewModel
class OtpViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val auth: AuthRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<OtpRoute>()
    private var challengeId = route.challengeId
    private val _state = MutableStateFlow(OtpUiState(route.phone, route.codeLength))
    val state = _state.asStateFlow()

    init { startCountdown(Instant.ofEpochMilli(route.resendAtEpochMs)) }

    private fun startCountdown(until: Instant) = viewModelScope.launch {
        while (true) {
            val left = ((until.toEpochMilli() - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            _state.update { it.copy(secondsToResend = left) }
            if (left == 0) break
            delay(1000)
        }
    }

    fun onCodeChange(value: String) {
        val digits = value.map { c -> if (c in '০'..'৯') '0' + (c - '০') else c }.filter { it.isDigit() }.joinToString("").take(_state.value.codeLength)
        _state.update { it.copy(code = digits, error = null) }
        if (digits.length == _state.value.codeLength) verify() // auto-submit on the last digit
    }

    fun verify() {
        val s = _state.value
        if (s.code.length != s.codeLength || s.verifying) return
        viewModelScope.launch {
            _state.update { it.copy(verifying = true) }
            val r = auth.verifyOtp(challengeId, s.code)
            _state.update { it.copy(verifying = false, error = (r as? AppResult.Failure)?.error, code = if (r is AppResult.Failure) "" else it.code) }
        }
    }

    fun resend() = viewModelScope.launch {
        when (val r = auth.requestOtp(_state.value.phone)) {
            is AppResult.Success -> { challengeId = r.data.challengeId; startCountdown(r.data.resendAvailableAt) }
            is AppResult.Failure -> _state.update { it.copy(error = r.error) }
        }
    }
}

data class EmailUiState(
    val register: Boolean = false,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val showValidation: Boolean = false,
    val error: AppError? = null,
    val resetSentTo: String? = null,
) {
    val emailValid get() = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email.trim())
    val passwordValid get() = password.length >= 8
    val nameValid get() = !register || name.isNotBlank()
}

@HiltViewModel
class EmailViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(EmailUiState())
    val state = _state.asStateFlow()

    fun setRegister(register: Boolean) = _state.update { it.copy(register = register, error = null, showValidation = false) }
    fun onName(v: String) = _state.update { it.copy(name = v, error = null) }
    fun onEmail(v: String) = _state.update { it.copy(email = v, error = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, error = null) }

    fun submit() {
        val s = _state.value
        if (!s.emailValid || !s.passwordValid || !s.nameValid) { _state.update { it.copy(showValidation = true) }; return }
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val r = if (s.register) auth.registerWithEmail(s.name, s.email, s.password) else auth.signInWithEmail(s.email, s.password)
            _state.update { it.copy(loading = false, error = (r as? AppResult.Failure)?.error) }
        }
    }

    fun forgot() {
        val s = _state.value
        if (!s.emailValid) { _state.update { it.copy(showValidation = true) }; return }
        viewModelScope.launch {
            auth.requestPasswordReset(s.email)
            _state.update { it.copy(resetSentTo = s.email.trim()) }
        }
    }
}
