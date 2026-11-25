package app.prostuti.core.domain.usecase

import app.prostuti.core.common.AppResult
import app.prostuti.core.domain.repository.MockExamRepository
import app.prostuti.core.domain.repository.RevisionRepository
import app.prostuti.core.model.Flashcard
import app.prostuti.core.model.MockAttempt
import app.prostuti.core.model.MockResult
import app.prostuti.core.model.RecallGrade
import javax.inject.Inject

/**
 * Submits a mock attempt. Retries transient failures a few times because a lost submission is the
 * worst possible outcome for a student; the backend makes submission idempotent per attempt id.
 */
class SubmitMockExamUseCase @Inject constructor(
    private val repository: MockExamRepository,
) {
    suspend operator fun invoke(attemptId: String, answers: Map<String, Set<String>>, attempts: Int = 3): AppResult<MockResult> {
        var last: AppResult<MockResult> = repository.submit(attemptId, answers)
        var tries = 1
        while (last is AppResult.Failure && tries < attempts && isTransient(last)) {
            kotlinx.coroutines.delay(1_000L * tries)
            last = repository.submit(attemptId, answers)
            tries++
        }
        return last
    }

    private fun isTransient(result: AppResult.Failure) =
        result.error.let { it is app.prostuti.core.common.AppError.NoConnection || it is app.prostuti.core.common.AppError.Timeout ||
            (it is app.prostuti.core.common.AppError.Server && it.httpCode >= 500) }
}

class StartMockExamUseCase @Inject constructor(private val repository: MockExamRepository) {
    suspend operator fun invoke(mockId: String): AppResult<MockAttempt> = repository.startAttempt(mockId)
}

class ReviewFlashcardUseCase @Inject constructor(private val repository: RevisionRepository) {
    suspend operator fun invoke(card: Flashcard, grade: RecallGrade): Flashcard = repository.review(card, grade)
}
