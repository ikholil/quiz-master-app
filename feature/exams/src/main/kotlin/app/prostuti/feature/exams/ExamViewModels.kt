package app.prostuti.feature.exams

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.prostuti.core.common.AppResult
import app.prostuti.core.domain.repository.ExamRepository
import app.prostuti.core.domain.repository.ProfileRepository
import app.prostuti.core.model.*
import app.prostuti.core.ui.LoadViewModel
import app.prostuti.core.ui.UiState
import app.prostuti.core.ui.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DirectoryState(
    val filter: ExamFilter = ExamFilter(),
    val exams: UiState<List<ExamSummary>> = UiState.Loading,
    val nextCursor: String? = null,
    val loadingMore: Boolean = false,
    val compareMode: Boolean = false,
    val compareIds: List<String> = emptyList(),
)

@HiltViewModel
class ExamDirectoryViewModel @Inject constructor(private val repo: ExamRepository) : ViewModel() {
    private val _state = MutableStateFlow(DirectoryState())
    val state = _state.asStateFlow()
    private var searchJob: Job? = null

    init { search(debounce = false) }

    fun onQuery(q: String) { _state.update { it.copy(filter = it.filter.copy(query = q)) }; search() }
    fun onCategory(c: ExamCategory?) { _state.update { it.copy(filter = it.filter.copy(category = c, bookmarkedOnly = false)) }; search(false) }
    fun onSavedOnly() { _state.update { it.copy(filter = it.filter.copy(bookmarkedOnly = true, category = null)) }; search(false) }

    /** Debounced, and each new query cancels the in-flight request. */
    fun search(debounce: Boolean = true) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce) delay(300)
            val r = repo.exams(_state.value.filter)
            _state.update {
                when (r) {
                    is AppResult.Success -> it.copy(exams = UiState.Success(r.data.items), nextCursor = r.data.nextCursor)
                    is AppResult.Failure -> it.copy(exams = UiState.Error(r.error))
                }
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        val cursor = s.nextCursor ?: return
        if (s.loadingMore) return
        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true) }
            val r = repo.exams(s.filter, cursor)
            _state.update {
                val current = (it.exams as? UiState.Success)?.data.orEmpty()
                if (r is AppResult.Success) it.copy(exams = UiState.Success(current + r.data.items), nextCursor = r.data.nextCursor, loadingMore = false)
                else it.copy(loadingMore = false)
            }
        }
    }

    fun toggleBookmark(exam: ExamSummary) {
        val flipped = !exam.isBookmarked
        _state.update { s -> s.copy(exams = (s.exams as? UiState.Success)?.let { UiState.Success(it.data.map { e -> if (e.id == exam.id) e.copy(isBookmarked = flipped) else e }) } ?: s.exams) }
        viewModelScope.launch { repo.setBookmarked(exam.id, flipped) }
    }

    fun toggleCompareMode() = _state.update { it.copy(compareMode = !it.compareMode, compareIds = emptyList()) }
    fun toggleCompare(id: String) = _state.update {
        val ids = if (id in it.compareIds) it.compareIds - id else (it.compareIds + id).takeLast(3)
        it.copy(compareIds = ids)
    }
}

@HiltViewModel
class ExamDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: ExamRepository,
    private val profiles: ProfileRepository,
) : LoadViewModel<ExamDetail>() {
    private val examId = savedStateHandle.toRoute<ExamDetailRoute>().examId
    init { load() }
    override suspend fun fetch() = repo.exam(examId)

    fun toggleBookmark() {
        var target = false
        update { d -> target = !d.summary.isBookmarked; d.copy(summary = d.summary.copy(isBookmarked = target)) }
        launchAction { repo.setBookmarked(examId, target) }
    }

    fun addTarget(onDone: () -> Unit) = viewModelScope.launch {
        val detail = (state.value as? UiState.Success)?.data ?: return@launch
        val current = profiles.profile.first()?.targets.orEmpty()
        if (current.any { it.examId == examId }) return@launch
        val next = current + TargetExam(examId, detail.summary.name, detail.deadlines.maxByOrNull { it.date }?.date, isPrimary = current.isEmpty())
        when (val r = repo.setTargets(next)) {
            is AppResult.Success -> { update { it.copy(summary = it.summary.copy(isTarget = true)) }; onDone() }
            is AppResult.Failure -> report(r.error)
        }
    }
}

@HiltViewModel
class CompareViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: ExamRepository,
) : LoadViewModel<SyllabusComparison>() {
    private val ids = savedStateHandle.toRoute<CompareRoute>().examIds
    init { load() }
    override suspend fun fetch() = repo.compare(ids)
}

@Suppress("unused") private val unused: (AppResult<Unit>) -> UiState<Unit> = { it.toUiState() }
