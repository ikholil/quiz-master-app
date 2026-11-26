package app.prostuti.feature.practice

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.prostuti.core.common.AppError
import app.prostuti.core.common.AppResult
import app.prostuti.core.common.getOrNull
import app.prostuti.core.domain.exam.NegativeMarking
import app.prostuti.core.domain.repository.LearningRepository
import app.prostuti.core.domain.repository.PracticeRepository
import app.prostuti.core.domain.repository.ProfileRepository
import app.prostuti.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import javax.inject.Inject

/** Sensible defaults per mode; a custom route overrides them. */
internal fun PracticeSessionRoute.toConfig(): PracticeConfig {
    val mode = runCatching { PracticeMode.valueOf(this.mode) }.getOrDefault(PracticeMode.CUSTOM)
    val (defCount, defMinutes) = when (mode) {
        PracticeMode.SPEED -> 10 to 5
        PracticeMode.MARATHON -> 50 to null
        PracticeMode.DAILY_CHALLENGE -> 10 to 8
        PracticeMode.TOPIC -> 10 to null
        PracticeMode.WEAK_TOPICS, PracticeMode.ACCURACY -> 15 to null
        else -> 20 to null
    }
    return PracticeConfig(
        mode = mode,
        subjectIds = subjectIds,
        topicIds = listOfNotNull(topicId),
        questionCount = if (count > 0) count else defCount,
        difficulty = difficulty?.let { runCatching { Difficulty.valueOf(it) }.getOrNull() },
        durationMinutes = if (minutes >= 0) minutes.takeIf { it > 0 } else defMinutes,
        instantFeedback = instantFeedback && mode != PracticeMode.SPEED,
        offlineOnly = offlineOnly,
        setId = setId,
    )
}

data class HubState(
    val subjects: List<Subject> = emptyList(),
    val examId: String? = null,
)

@HiltViewModel
class PracticeHubViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val learning: LearningRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(HubState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val examId = (profiles.profile.first() ?: profiles.refresh().getOrNull())?.primaryTarget?.examId ?: return@launch
            _state.update { it.copy(examId = examId, subjects = learning.subjects(examId).getOrNull().orEmpty()) }
        }
    }
}

sealed interface SessionPhase {
    data object Loading : SessionPhase
    data class Failed(val error: AppError) : SessionPhase
    data object Active : SessionPhase
    data object Submitting : SessionPhase
    data class Finished(val result: PracticeResult) : SessionPhase
}

data class PracticeUiState(
    val phase: SessionPhase = SessionPhase.Loading,
    val session: PracticeSession? = null,
    val index: Int = 0,
    val answers: Map<String, Set<String>> = emptyMap(),
    /** Questions whose answer has been checked (instant-feedback mode). */
    val checked: Set<String> = emptySet(),
    val remainingSeconds: Long? = null,
    val elapsedSeconds: Long = 0,
) {
    val questions get() = session?.questions.orEmpty()
    val current: Question? get() = questions.getOrNull(index)
    val instantFeedback get() = session?.config?.instantFeedback == true
    val isLast get() = index == questions.lastIndex
    val currentChecked get() = current?.id in checked
    val currentCorrect: Boolean? get() = current?.let { q ->
        if (q.id !in checked) null else NegativeMarking.isAnswerCorrect(answers[q.id].orEmpty(), q.solution?.correctOptionIds.orEmpty())
    }
}

@HiltViewModel
class PracticeSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: PracticeRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<PracticeSessionRoute>()
    private val _state = MutableStateFlow(PracticeUiState())
    val state = _state.asStateFlow()

    private val timeSpent = mutableMapOf<String, Long>()
    private var shownAt = 0L
    private var startedAt = 0L
    private var timerJob: Job? = null

    init { start() }

    fun start() = viewModelScope.launch {
        _state.update { PracticeUiState() }
        var config = route.toConfig()
        if (config.mode == PracticeMode.DAILY_CHALLENGE) repo.dailyChallenge().getOrNull()?.let { config = it }
        when (val r = repo.start(config)) {
            is AppResult.Success -> {
                startedAt = SystemClock.elapsedRealtime()
                shownAt = startedAt
                _state.update { it.copy(phase = SessionPhase.Active, session = r.data, remainingSeconds = config.durationMinutes?.let { m -> m * 60L }) }
                startTimer(config.durationMinutes)
            }
            is AppResult.Failure -> _state.update { it.copy(phase = SessionPhase.Failed(r.error)) }
        }
    }

    /** Practice is unofficial, so a monotonic device timer is fine (mock exams use the server clock). */
    private fun startTimer(minutes: Int?) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = (SystemClock.elapsedRealtime() - startedAt) / 1000
                val remaining = minutes?.let { (it * 60L - elapsed).coerceAtLeast(0) }
                _state.update { it.copy(elapsedSeconds = elapsed, remainingSeconds = remaining) }
                if (remaining == 0L) { finish(); break }
                delay(1000)
            }
        }
    }

    private fun recordTime() {
        val q = _state.value.current ?: return
        val now = SystemClock.elapsedRealtime()
        timeSpent[q.id] = (timeSpent[q.id] ?: 0) + (now - shownAt)
        shownAt = now
    }

    fun select(optionId: String) {
        val s = _state.value
        val q = s.current ?: return
        if (s.phase != SessionPhase.Active || q.id in s.checked) return
        val current = s.answers[q.id].orEmpty()
        val next = if (q.type == QuestionType.MULTI_SELECT) {
            if (optionId in current) current - optionId else current + optionId
        } else setOf(optionId)
        _state.update { it.copy(answers = it.answers + (q.id to next)) }
        // Single-answer questions are checked immediately in instant-feedback mode.
        if (s.instantFeedback && q.type != QuestionType.MULTI_SELECT) check()
    }

    fun check() {
        val q = _state.value.current ?: return
        _state.update { it.copy(checked = it.checked + q.id) }
    }

    fun next() {
        recordTime()
        val s = _state.value
        if (s.isLast) finish() else _state.update { it.copy(index = it.index + 1) }
    }

    fun previous() {
        recordTime()
        _state.update { it.copy(index = (it.index - 1).coerceAtLeast(0)) }
    }

    fun finish() {
        val s = _state.value
        val session = s.session ?: return
        if (s.phase != SessionPhase.Active) return
        recordTime()
        timerJob?.cancel()
        _state.update { it.copy(phase = SessionPhase.Submitting) }
        viewModelScope.launch {
            val answers = session.questions.map { q -> AnswerRecord(q.id, s.answers[q.id].orEmpty(), Duration.ofMillis(timeSpent[q.id] ?: 0)) }
            when (val r = repo.submit(session, answers)) {
                is AppResult.Success -> _state.update { it.copy(phase = SessionPhase.Finished(r.data)) }
                is AppResult.Failure -> _state.update { it.copy(phase = SessionPhase.Active) }.also { startTimer(session.config.durationMinutes) }
            }
        }
    }
}
