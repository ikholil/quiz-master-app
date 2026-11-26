package app.prostuti.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.prostuti.core.common.AppError
import app.prostuti.core.common.AppResult
import app.prostuti.core.common.getOrNull
import app.prostuti.core.domain.repository.ExamRepository
import app.prostuti.core.domain.repository.LearningRepository
import app.prostuti.core.domain.repository.ProfileRepository
import app.prostuti.core.domain.repository.SettingsRepository
import app.prostuti.core.model.*
import app.prostuti.core.ui.UiState
import app.prostuti.core.ui.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class OnboardingStep { ABOUT, EXAMS, DATE, TIME, LEVEL, SUBJECTS, PLAN }
enum class SubjectStrength { WEAK, OK, STRONG }

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.ABOUT,
    val editMode: Boolean = false,
    val name: String = "",
    val language: AppLanguage = AppLanguage.BENGALI,
    val exams: UiState<List<ExamSummary>> = UiState.Loading,
    val selectedExamIds: List<String> = emptyList(),
    val primaryExamId: String? = null,
    val examDate: LocalDate? = null,
    val dateUnknown: Boolean = false,
    val dailyMinutes: Int = 60,
    val level: PreparationLevel? = null,
    val subjects: UiState<List<Subject>> = UiState.Loading,
    val strengths: Map<String, SubjectStrength> = emptyMap(),
    val plan: UiState<List<PlanTask>> = UiState.Loading,
    val saving: Boolean = false,
    val error: AppError? = null,
) {
    val primaryExam: ExamSummary? get() = (exams as? UiState.Success)?.data?.firstOrNull { it.id == primaryExamId }

    val canContinue: Boolean get() = when (step) {
        OnboardingStep.ABOUT -> name.isNotBlank()
        OnboardingStep.EXAMS -> selectedExamIds.isNotEmpty()
        OnboardingStep.DATE -> dateUnknown || examDate != null
        OnboardingStep.TIME -> dailyMinutes > 0
        OnboardingStep.LEVEL -> level != null
        OnboardingStep.SUBJECTS -> true
        OnboardingStep.PLAN -> plan is UiState.Success
    }

    fun preferences(): PreparationPreferences {
        val examList = (exams as? UiState.Success)?.data.orEmpty()
        return PreparationPreferences(
            fullName = name.trim(),
            language = language,
            targets = selectedExamIds.mapNotNull { id -> examList.firstOrNull { it.id == id } }.map {
                TargetExam(it.id, it.name, if (it.id == primaryExamId && !dateUnknown) examDate else null, it.id == primaryExamId)
            },
            dailyStudyMinutes = dailyMinutes,
            level = level ?: PreparationLevel.BEGINNER,
            strongSubjectIds = strengths.filterValues { it == SubjectStrength.STRONG }.keys.toList(),
            weakSubjectIds = strengths.filterValues { it == SubjectStrength.WEAK }.keys.toList(),
        )
    }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val profiles: ProfileRepository,
    private val examRepository: ExamRepository,
    private val learning: LearningRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val editMode = savedStateHandle.toRoute<OnboardingRoute>().edit
    private val _state = MutableStateFlow(OnboardingState(editMode = editMode))
    val state = _state.asStateFlow()
    private val _finished = Channel<Unit>(Channel.BUFFERED)
    val finished = _finished.receiveAsFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(language = settings.settings.first().language) }
            val profile = profiles.profile.first() ?: profiles.refresh().getOrNull()
            if (profile != null) prefill(profile)
            loadExams()
        }
    }

    private fun prefill(p: StudentProfile) = _state.update {
        it.copy(
            name = p.fullName,
            selectedExamIds = p.targets.map { t -> t.examId },
            primaryExamId = p.primaryTarget?.examId,
            examDate = p.primaryTarget?.examDate,
            dateUnknown = p.targets.isNotEmpty() && p.primaryTarget?.examDate == null,
            dailyMinutes = p.dailyStudyMinutes,
            level = p.level,
            strengths = p.weakSubjectIds.associateWith { SubjectStrength.WEAK } + p.strongSubjectIds.associateWith { SubjectStrength.STRONG },
        )
    }

    fun loadExams() = viewModelScope.launch {
        _state.update { it.copy(exams = UiState.Loading) }
        val r = examRepository.exams(ExamFilter())
        _state.update { it.copy(exams = when (r) { is AppResult.Success -> UiState.Success(r.data.items); is AppResult.Failure -> UiState.Error(r.error) }) }
    }

    fun onName(v: String) = _state.update { it.copy(name = v.take(60)) }
    fun onLanguage(l: AppLanguage) {
        _state.update { it.copy(language = l) }
        viewModelScope.launch { settings.setLanguage(l) }
    }

    fun toggleExam(id: String) = _state.update { s ->
        val selected = if (id in s.selectedExamIds) s.selectedExamIds - id else s.selectedExamIds + id
        val primary = when {
            selected.isEmpty() -> null
            s.primaryExamId in selected -> s.primaryExamId
            else -> selected.first()
        }
        val suggestedDate = if (primary != s.primaryExamId) (s.exams as? UiState.Success)?.data?.firstOrNull { it.id == primary }?.nextDeadline?.date else s.examDate
        s.copy(selectedExamIds = selected, primaryExamId = primary, examDate = suggestedDate ?: s.examDate)
    }

    fun setPrimary(id: String) = _state.update { it.copy(primaryExamId = id) }
    fun setDate(date: LocalDate?) = _state.update { it.copy(examDate = date, dateUnknown = false) }
    fun setDateUnknown(unknown: Boolean) = _state.update { it.copy(dateUnknown = unknown) }
    fun setMinutes(m: Int) = _state.update { it.copy(dailyMinutes = m) }
    fun setLevel(l: PreparationLevel) = _state.update { it.copy(level = l) }
    fun setStrength(subjectId: String, s: SubjectStrength) = _state.update { it.copy(strengths = it.strengths + (subjectId to s)) }

    fun back(): Boolean {
        val s = _state.value
        if (s.step.ordinal == 0) return false
        _state.update { it.copy(step = OnboardingStep.entries[s.step.ordinal - 1]) }
        return true
    }

    fun next() {
        val s = _state.value
        if (!s.canContinue) return
        when (s.step) {
            OnboardingStep.LEVEL -> loadSubjects()
            OnboardingStep.SUBJECTS -> buildPlan()
            OnboardingStep.PLAN -> { finish(); return }
            else -> Unit
        }
        _state.update { it.copy(step = OnboardingStep.entries[s.step.ordinal + 1]) }
    }

    private fun loadSubjects() = viewModelScope.launch {
        val examId = _state.value.primaryExamId ?: return@launch
        _state.update { it.copy(subjects = UiState.Loading) }
        val r = learning.subjects(examId).toUiState()
        _state.update { it.copy(subjects = r) }
    }

    fun buildPlan() = viewModelScope.launch {
        _state.update { it.copy(plan = UiState.Loading) }
        val prefs = _state.value.preferences()
        val saved = profiles.savePreferences(prefs)
        if (saved is AppResult.Failure) { _state.update { it.copy(plan = UiState.Error(saved.error)) }; return@launch }
        examRepository.setTargets(prefs.targets)
        _state.update { it.copy(plan = profiles.previewInitialPlan(prefs).toUiState()) }
    }

    private fun finish() = viewModelScope.launch {
        _state.update { it.copy(saving = true) }
        val r = if (editMode) AppResult.Success(Unit) else profiles.completeOnboarding()
        _state.update { it.copy(saving = false, error = (r as? AppResult.Failure)?.error) }
        if (r is AppResult.Success) _finished.send(Unit)
    }
}
