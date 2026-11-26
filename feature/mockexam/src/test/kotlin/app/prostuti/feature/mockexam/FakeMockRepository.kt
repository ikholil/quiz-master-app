package app.prostuti.feature.mockexam

import app.prostuti.core.common.AppError
import app.prostuti.core.common.AppResult
import app.prostuti.core.common.TimeSource
import app.prostuti.core.domain.repository.MockExamRepository
import app.prostuti.core.model.*
import java.time.Duration
import java.time.Instant

internal class FakeTime : TimeSource {
    var monotonic = 0L
    var wall: Instant = Instant.parse("2026-01-01T00:00:00Z")
    override fun elapsedRealtimeMillis() = monotonic
    override fun wallClock() = wall
}

internal fun question(id: String, correct: String = "$id-a") = Question(
    id, QuestionType.MCQ, LocalizedText("Question $id", "প্রশ্ন $id"),
    listOf("a", "b", "c", "d").map { QuestionOption("$id-$it", LocalizedText("Option $it")) },
    null, null, null, Difficulty.MEDIUM, QuestionProvenance.OFFICIAL, "t", LocalizedText("Topic"), LocalizedText("Subject"),
    emptyList(), emptyList(), solution = null,
)

internal val serverNow: Instant = Instant.parse("2026-11-25T10:00:00Z")

internal fun attempt(minutes: Long = 10, rules: MockRules = MockRules(true, false, false, false, true)): MockAttempt {
    val s1 = MockSection("s1", LocalizedText("Bangla"), 2, 1.0, 0.5, null)
    val s2 = MockSection("s2", LocalizedText("English"), 2, 1.0, 0.5, null)
    val mock = MockExamSummary("m1", LocalizedText("Model test"), "bcs", "BCS", true, minutes.toInt(), 4.0, 4, listOf(s1, s2), rules, 0, null, false, null)
    return MockAttempt(
        "att-1", mock,
        listOf(AttemptSection(s1, listOf(question("q1"), question("q2"))), AttemptSection(s2, listOf(question("q3"), question("q4")))),
        serverNow, serverNow.plus(Duration.ofMinutes(minutes)), emptyMap(), emptySet(),
    )
}

internal class FakeMockRepository(var attempt: MockAttempt = attempt()) : MockExamRepository {
    val saves = mutableListOf<Map<String, Set<String>>>()
    val submissions = mutableListOf<Map<String, Set<String>>>()
    var submitFailuresRemaining = 0
    var startCalls = 0
    var resumeCalls = 0

    override suspend fun mocks(examId: String?) = AppResult.Success(listOf(attempt.mock))
    override suspend fun mock(mockId: String) = AppResult.Success(attempt.mock)
    override suspend fun startAttempt(mockId: String): AppResult<MockAttempt> { startCalls++; return AppResult.Success(attempt) }
    override suspend fun resumeAttempt(attemptId: String): AppResult<MockAttempt> { resumeCalls++; return AppResult.Success(attempt) }
    override suspend fun saveAnswers(attemptId: String, answers: Map<String, Set<String>>, markedForReview: Set<String>): AppResult<Unit> {
        saves += answers; return AppResult.Success(Unit)
    }
    override suspend fun submit(attemptId: String, answers: Map<String, Set<String>>): AppResult<MockResult> {
        if (submitFailuresRemaining > 0) { submitFailuresRemaining--; return AppResult.Failure(AppError.NoConnection) }
        submissions += answers
        return AppResult.Success(
            MockResult(attemptId, attempt.mock, serverNow, Duration.ofMinutes(5), 0.0, 4.0, 0.0, emptyList(), null, null, null, null, emptyList(), answers, emptyList()),
        )
    }
    override suspend fun result(attemptId: String) = AppResult.Failure(AppError.NotFound)
    override suspend fun history() = AppResult.Success(emptyList<MockAttemptHistoryItem>())
}
