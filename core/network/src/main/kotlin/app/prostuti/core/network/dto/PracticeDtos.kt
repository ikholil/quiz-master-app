package app.prostuti.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PracticeConfigDto(
    val mode: String,
    val subjectIds: List<String> = emptyList(),
    val topicIds: List<String> = emptyList(),
    val questionCount: Int,
    val difficulty: String? = null,
    val durationMinutes: Int? = null,
    val instantFeedback: Boolean = true,
    val setId: String? = null,
)

@Serializable data class PracticeSessionDto(val id: String, val config: PracticeConfigDto, val questions: List<QuestionDto>)
@Serializable data class AnswerDto(val questionId: String, val selectedOptionIds: List<String>, val timeSpentMs: Long)
@Serializable data class SubmitPracticeDto(val answers: List<AnswerDto>, val clientSessionId: String? = null)

@Serializable data class TopicOutcomeDto(val topicId: String, val topicName: LocalizedDto, val correct: Int, val total: Int)

@Serializable
data class PracticeResultDto(
    val sessionId: String,
    val mode: String,
    val correct: Int,
    val incorrect: Int,
    val skipped: Int,
    val score: Double,
    val maxScore: Double,
    val timeSpentMs: Long,
    val topicOutcomes: List<TopicOutcomeDto>,
    val questions: List<QuestionDto>,
    val answers: List<AnswerDto>,
    val nextAction: RecommendationDto? = null,
)

@Serializable
data class PracticeSetDto(
    val id: String,
    val title: LocalizedDto,
    val questionCount: Int,
    val estimatedMinutes: Int,
    val reason: LocalizedDto,
)

@Serializable data class QuestionSetBundleDto(val set: PracticeSetDto, val questions: List<QuestionDto>)
