package app.prostuti.feature.mockexam

import androidx.lifecycle.SavedStateHandle
import app.prostuti.core.common.AppResult
import app.prostuti.core.domain.repository.MockExamRepository
import app.prostuti.core.model.MockAttemptHistoryItem
import app.prostuti.core.model.MockExamSummary
import app.prostuti.core.model.MockResult
import app.prostuti.core.ui.LoadViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MockListViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val repo: MockExamRepository) : LoadViewModel<List<MockExamSummary>>() {
    private val examId: String? = savedStateHandle["examId"]
    init { load() }
    override suspend fun fetch() = repo.mocks(examId)
}

@HiltViewModel
class MockInstructionsViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val repo: MockExamRepository) : LoadViewModel<MockExamSummary>() {
    private val mockId: String = checkNotNull(savedStateHandle["mockId"])
    init { load() }
    override suspend fun fetch() = repo.mock(mockId)
}

@HiltViewModel
class MockResultViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val repo: MockExamRepository) : LoadViewModel<MockResult>() {
    private val attemptId: String = checkNotNull(savedStateHandle["attemptId"])
    init { load() }
    override suspend fun fetch(): AppResult<MockResult> = repo.result(attemptId)
}

@HiltViewModel
class MockHistoryViewModel @Inject constructor(private val repo: MockExamRepository) : LoadViewModel<List<MockAttemptHistoryItem>>() {
    init { load() }
    override suspend fun fetch() = repo.history()
}
