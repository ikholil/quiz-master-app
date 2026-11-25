package app.prostuti.core.model

import java.time.Duration
import java.time.Instant

data class MockSection(
    val id: String,
    val name: LocalizedText,
    val questionCount: Int,
    val marksPerQuestion: Double,
    val negativeMarkPerWrong: Double,
    /** Null when sections share the overall timer. */
    val durationMinutes: Int?,
)

data class MockRules(
    val allowSectionSwitching: Boolean,
    val revealAnswersDuringExam: Boolean,
    val shuffleQuestions: Boolean,
    val shuffleOptions: Boolean,
    val autoSubmitOnTimeout: Boolean,
)

data class MockExamSummary(
    val id: String,
    val title: LocalizedText,
    val examId: String,
    val examShortName: String,
    val isFullLength: Boolean,
    val durationMinutes: Int,
    val totalMarks: Double,
    val questionCount: Int,
    val sections: List<MockSection>,
    val rules: MockRules,
    val attemptCount: Int,
    val bestScore: Double?,
    val isPremium: Boolean,
    val availableFrom: Instant?,
)

/**
 * A live attempt. Timing is anchored to the server: [serverNow] is the server clock at the moment
 * the attempt was fetched and [endsAt] is the server-side deadline. The device clock is never trusted.
 * Questions arrive without solutions unless [MockRules.revealAnswersDuringExam] is set.
 */
data class MockAttempt(
    val attemptId: String,
    val mock: MockExamSummary,
    val sections: List<AttemptSection>,
    val serverNow: Instant,
    val endsAt: Instant,
    val savedAnswers: Map<String, Set<String>>,
    val markedForReview: Set<String>,
)

data class AttemptSection(val section: MockSection, val questions: List<Question>)

data class SectionResult(
    val section: MockSection,
    val correct: Int,
    val incorrect: Int,
    val skipped: Int,
    val score: Double,
    val negativeMarks: Double,
    val maxScore: Double,
)

data class MockResult(
    val attemptId: String,
    val mock: MockExamSummary,
    val submittedAt: Instant,
    val timeTaken: Duration,
    val score: Double,
    val maxScore: Double,
    val negativeMarks: Double,
    val sectionResults: List<SectionResult>,
    val percentile: Float?,
    val rank: Int?,
    val totalCandidates: Int?,
    val passMarks: Double?,
    val questions: List<Question>,
    val answers: Map<String, Set<String>>,
    val recommendations: List<Recommendation>,
) {
    val correct: Int get() = sectionResults.sumOf { it.correct }
    val incorrect: Int get() = sectionResults.sumOf { it.incorrect }
    val skipped: Int get() = sectionResults.sumOf { it.skipped }
}

data class MockAttemptHistoryItem(
    val attemptId: String,
    val mockTitle: LocalizedText,
    val submittedAt: Instant,
    val score: Double,
    val maxScore: Double,
    val negativeMarks: Double,
    val percentile: Float?,
)
