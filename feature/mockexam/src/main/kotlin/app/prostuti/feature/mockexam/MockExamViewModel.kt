package app.prostuti.feature.mockexam

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.prostuti.core.common.AppError
import app.prostuti.core.common.AppResult
import app.prostuti.core.common.TimeSource
import app.prostuti.core.domain.exam.ExamSessionState
import app.prostuti.core.domain.exam.ServerAnchoredClock
import app.prostuti.core.domain.repository.MockExamRepository
import app.prostuti.core.domain.usecase.StartMockExamUseCase
import app.prostuti.core.domain.usecase.SubmitMockExamUseCase
import app.prostuti.core.model.MockAttempt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import javax.inject.Inject

sealed interface ExamPhase {
    data object Starting : ExamPhase
    data class Failed(val error: AppError) : ExamPhase
    data object Running : ExamPhase
    data class Submitting(val timeUp: Boolean) : ExamPhase
    data class SubmitFailed(val error: AppError) : ExamPhase
    data class Submitted(val attemptId: String) : ExamPhase
}

enum class SaveStatus { IDLE, SAVING, SAVED, FAILED }

data class MockExamUiState(
    val phase: ExamPhase = ExamPhase.Starting,
    val attempt: MockAttempt? = null,
    val session: ExamSessionState? = null,
    val remaining: Duration = Duration.ZERO,
    val saveStatus: SaveStatus = SaveStatus.IDLE,
)

/**
 * Drives a live mock exam. Integrity rules:
 * - Remaining time comes from [ServerAnchoredClock] (server time + monotonic elapsed), never the wall clock.
 * - Answers autosave every few seconds and whenever the app goes to the background.
 * - Questions arrive without solutions; nothing here can reveal answers before submission.
 * - Submission is idempotent server-side and retried on transient failures.
 * - The attempt id is kept in [SavedStateHandle] so process death resumes the same attempt.
 */
@HiltViewModel
class MockExamViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val startExam: StartMockExamUseCase,
    private val repo: MockExamRepository,
    private val submitExam: SubmitMockExamUseCase,
    private val timeSource: TimeSource,
) : ViewModel() {
    private val mockId: String = checkNotNull(savedStateHandle["mockId"])
    private val _state = MutableStateFlow(MockExamUiState())
    val state = _state.asStateFlow()

    private var clock: ServerAnchoredClock? = null
    private var tickerJob: Job? = null
    private var autosaveJob: Job? = null
    private var saveJob: Job? = null

    init { start() }

    fun start() = viewModelScope.launch {
        _state.update { it.copy(phase = ExamPhase.Starting) }
        val existing: String? = savedStateHandle[KEY_ATTEMPT]
        val result = if (existing != null) repo.resumeAttempt(existing) else startExam(mockId)
        when (result) {
            is AppResult.Success -> begin(result.data)
            is AppResult.Failure -> _state.update { it.copy(phase = ExamPhase.Failed(result.error)) }
        }
    }

    private fun begin(attempt: MockAttempt) {
        savedStateHandle[KEY_ATTEMPT] = attempt.attemptId
        val session = ExamSessionState(
            sections = attempt.sections,
            answers = attempt.savedAnswers,
            markedForReview = attempt.markedForReview,
            allowSectionSwitching = attempt.mock.rules.allowSectionSwitching,
        ).visitCurrent()
        clock = ServerAnchoredClock(timeSource, attempt.serverNow, attempt.endsAt)
        _state.update { it.copy(phase = ExamPhase.Running, attempt = attempt, session = session, remaining = clock!!.remaining()) }
        startTicker()
        startAutosave()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive && _state.value.phase == ExamPhase.Running) {
                val remaining = clock?.remaining() ?: Duration.ZERO
                _state.update { it.copy(remaining = remaining) }
                if (remaining.isZero) {
                    if (_state.value.attempt?.mock?.rules?.autoSubmitOnTimeout != false) submit(timeUp = true)
                    break
                }
                delay(TICK_MS)
            }
        }
    }

    private fun startAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            while (isActive && _state.value.phase == ExamPhase.Running) {
                delay(AUTOSAVE_MS)
                if (_state.value.session?.dirty == true) saveNow()
            }
        }
    }

    private fun mutate(transform: (ExamSessionState) -> ExamSessionState) {
        if (_state.value.phase != ExamPhase.Running) return
        _state.update { s -> s.session?.let { s.copy(session = transform(it)) } ?: s }
    }

    fun select(optionId: String) = mutate { it.select(optionId) }
    fun clear() = mutate { it.clearAnswer() }
    fun toggleReview() = mutate { it.toggleMarkForReview() }
    fun next() = mutate { it.next() }
    fun previous() = mutate { it.previous() }
    fun goTo(questionId: String) = mutate { it.goTo(questionId) }
    fun goToSection(index: Int) = mutate { it.goToSection(index) }

    /** Called on every background transition and periodically while dirty. */
    fun saveNow() {
        val s = _state.value
        val session = s.session ?: return
        val attemptId = s.attempt?.attemptId ?: return
        if (s.phase != ExamPhase.Running || !session.dirty || saveJob?.isActive == true) return
        val answers = session.answers
        val marked = session.markedForReview
        saveJob = viewModelScope.launch {
            _state.update { it.copy(saveStatus = SaveStatus.SAVING) }
            when (val r = repo.saveAnswers(attemptId, answers, marked)) {
                is AppResult.Success -> _state.update {
                    val current = it.session
                    // Only clear dirty if nothing changed while the request was in flight.
                    val unchanged = current != null && current.answers == answers && current.markedForReview == marked
                    it.copy(saveStatus = SaveStatus.SAVED, session = if (unchanged) current!!.markSaved() else current)
                }
                is AppResult.Failure -> {
                    _state.update { it.copy(saveStatus = SaveStatus.FAILED) }
                    // The server says time is over: submit what it accepts.
                    if (r.error is AppError.Conflict) submit(timeUp = true)
                }
            }
        }
    }

    fun submit(timeUp: Boolean = false) {
        val s = _state.value
        val attemptId = s.attempt?.attemptId ?: return
        val answers = s.session?.answers ?: return
        if (s.phase is ExamPhase.Submitting || s.phase is ExamPhase.Submitted) return
        tickerJob?.cancel()
        autosaveJob?.cancel()
        _state.update { it.copy(phase = ExamPhase.Submitting(timeUp)) }
        viewModelScope.launch {
            when (val r = submitExam(attemptId, answers)) {
                is AppResult.Success -> _state.update { it.copy(phase = ExamPhase.Submitted(r.data.attemptId)) }
                is AppResult.Failure -> _state.update { it.copy(phase = ExamPhase.SubmitFailed(r.error)) }
            }
        }
    }

    companion object {
        const val KEY_ATTEMPT = "attemptId"
        const val TICK_MS = 1_000L
        const val AUTOSAVE_MS = 5_000L
    }
}
