package app.prostuti.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class MockSectionDto(
    val id: String,
    val name: LocalizedDto,
    val questionCount: Int,
    val marksPerQuestion: Double,
    val negativeMarkPerWrong: Double,
    val durationMinutes: Int? = null,
)

@Serializable
data class MockRulesDto(
    val allowSectionSwitching: Boolean = true,
    val revealAnswersDuringExam: Boolean = false,
    val shuffleQuestions: Boolean = false,
    val shuffleOptions: Boolean = false,
    val autoSubmitOnTimeout: Boolean = true,
)

@Serializable
data class MockSummaryDto(
    val id: String,
    val title: LocalizedDto,
    val examId: String,
    val examShortName: String,
    val isFullLength: Boolean,
    val durationMinutes: Int,
    val totalMarks: Double,
    val questionCount: Int,
    val sections: List<MockSectionDto>,
    val rules: MockRulesDto,
    val attemptCount: Int = 0,
    val bestScore: Double? = null,
    val isPremium: Boolean = false,
    val availableFrom: String? = null,
)

@Serializable data class AttemptSectionDto(val section: MockSectionDto, val questions: List<QuestionDto>)
@Serializable data class SavedAnswerDto(val questionId: String, val selectedOptionIds: List<String>)

@Serializable
data class AttemptDto(
    val attemptId: String,
    val mock: MockSummaryDto,
    val sections: List<AttemptSectionDto>,
    val serverNow: String,
    val endsAt: String,
    val savedAnswers: List<SavedAnswerDto> = emptyList(),
    val markedForReview: List<String> = emptyList(),
)

@Serializable data class SaveAnswersDto(val answers: List<SavedAnswerDto>, val markedForReview: List<String>)
@Serializable data class SubmitAttemptDto(val answers: List<SavedAnswerDto>)

@Serializable
data class SectionResultDto(
    val section: MockSectionDto,
    val correct: Int,
    val incorrect: Int,
    val skipped: Int,
    val score: Double,
    val negativeMarks: Double,
    val maxScore: Double,
)

@Serializable
data class MockResultDto(
    val attemptId: String,
    val mock: MockSummaryDto,
    val submittedAt: String,
    val timeTakenMs: Long,
    val score: Double,
    val maxScore: Double,
    val negativeMarks: Double,
    val sectionResults: List<SectionResultDto>,
    val percentile: Float? = null,
    val rank: Int? = null,
    val totalCandidates: Int? = null,
    val passMarks: Double? = null,
    val questions: List<QuestionDto>,
    val answers: List<SavedAnswerDto>,
    val recommendations: List<RecommendationDto> = emptyList(),
)

@Serializable
data class AttemptHistoryDto(
    val attemptId: String,
    val mockTitle: LocalizedDto,
    val submittedAt: String,
    val score: Double,
    val maxScore: Double,
    val negativeMarks: Double,
    val percentile: Float? = null,
)
