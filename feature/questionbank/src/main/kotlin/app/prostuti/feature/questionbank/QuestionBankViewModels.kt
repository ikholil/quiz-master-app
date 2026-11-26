package app.prostuti.feature.questionbank

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.prostuti.core.common.AppResult
import app.prostuti.core.common.getOrNull
import app.prostuti.core.domain.repository.QuestionRepository
import app.prostuti.core.model.*
import app.prostuti.core.ui.LoadViewModel
import app.prostuti.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuestionListState(
    val filter: QuestionFilter,
    val questions: UiState<List<Question>> = UiState.Loading,
    val nextCursor: String? = null,
    val loadingMore: Boolean = false,
    val revealed: Set<String> = emptySet(),
    val selections: Map<String, Set<String>> = emptyMap(),
)

@HiltViewModel
class QuestionBankViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: QuestionRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<QuestionBankRoute>()
    private val _state = MutableStateFlow(QuestionListState(QuestionFilter(examId = route.examId, topicId = route.topicId)))
    val state = _state.asStateFlow()
    private var job: Job? = null

    init { refresh(false) }

    fun setFilter(transform: (QuestionFilter) -> QuestionFilter, debounce: Boolean = false) {
        _state.update { it.copy(filter = transform(it.filter)) }
        refresh(debounce)
    }

    fun refresh(debounce: Boolean = false) {
        job?.cancel()
        job = viewModelScope.launch {
            if (debounce) delay(300)
            _state.update { it.copy(questions = UiState.Loading) }
            val r = repo.questions(_state.value.filter)
            _state.update {
                when (r) {
                    is AppResult.Success -> it.copy(questions = UiState.Success(r.data.items), nextCursor = r.data.nextCursor)
                    is AppResult.Failure -> it.copy(questions = UiState.Error(r.error))
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
            val r = repo.questions(s.filter, cursor)
            _state.update {
                val current = (it.questions as? UiState.Success)?.data.orEmpty()
                if (r is AppResult.Success) it.copy(questions = UiState.Success(current + r.data.items), nextCursor = r.data.nextCursor, loadingMore = false)
                else it.copy(loadingMore = false)
            }
        }
    }

    /** Selecting an option reveals the answer: the bank is for self-testing, not passive reading. */
    fun select(question: Question, optionId: String) = _state.update {
        if (question.id in it.revealed) it
        else it.copy(selections = it.selections + (question.id to setOf(optionId)), revealed = it.revealed + question.id)
    }

    fun toggleReveal(id: String) = _state.update { it.copy(revealed = if (id in it.revealed) it.revealed - id else it.revealed + id) }
}

data class QuestionDetail(
    val question: Question,
    val similar: List<Question>,
    val collections: List<QuestionCollection>,
)

@HiltViewModel
class QuestionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: QuestionRepository,
) : LoadViewModel<QuestionDetail>() {
    val questionId = savedStateHandle.toRoute<QuestionDetailRoute>().questionId
    val selection = MutableStateFlow<Set<String>>(emptySet())
    val revealed = MutableStateFlow(false)

    init { load() }

    override suspend fun fetch(): AppResult<QuestionDetail> = when (val q = repo.question(questionId)) {
        is AppResult.Success -> AppResult.Success(
            QuestionDetail(q.data, repo.similar(questionId).getOrNull().orEmpty(), repo.collections().getOrNull().orEmpty()),
        )
        is AppResult.Failure -> q
    }

    fun select(optionId: String) {
        if (revealed.value) return
        val q = (state.value as? UiState.Success)?.data?.question ?: return
        selection.value = if (q.type == QuestionType.MULTI_SELECT) {
            if (optionId in selection.value) selection.value - optionId else selection.value + optionId
        } else setOf(optionId)
        if (q.type != QuestionType.MULTI_SELECT) revealed.value = true
    }

    fun reveal() { revealed.value = !revealed.value }

    fun toggleBookmark() {
        var b = false
        update { d -> b = !d.question.isBookmarked; d.copy(question = d.question.copy(isBookmarked = b)) }
        launchAction { repo.setBookmarked(questionId, b) }
    }

    fun saveNote(note: String) {
        update { it.copy(question = it.question.copy(note = note)) }
        launchAction { repo.saveNote(questionId, note) }
    }

    fun report(reason: QuestionReportReason, details: String, onDone: () -> Unit) = viewModelScope.launch {
        when (val r = repo.report(questionId, reason, details)) {
            is AppResult.Success -> onDone()
            is AppResult.Failure -> report(r.error)
        }
    }

    fun addToCollection(collection: QuestionCollection, onDone: (String) -> Unit) = viewModelScope.launch {
        if (repo.addToCollection(collection.id, questionId) is AppResult.Success) onDone(collection.name)
    }

    fun createCollection(name: String, onDone: (String) -> Unit) = viewModelScope.launch {
        val c = repo.createCollection(name).getOrNull() ?: return@launch
        update { it.copy(collections = it.collections + c) }
        addToCollection(c, onDone)
    }
}

@HiltViewModel
class WrongAnswersViewModel @Inject constructor(private val repo: QuestionRepository) : ViewModel() {
    val entries = repo.wrongAnswers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val showResolved = MutableStateFlow(false)
    val expanded = MutableStateFlow<Set<String>>(emptySet())

    fun toggle(id: String) = expanded.update { if (id in it) it - id else it + id }
    fun resolve(id: String) = viewModelScope.launch { repo.markWrongAnswerResolved(id) }
}
