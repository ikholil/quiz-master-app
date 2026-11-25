package app.prostuti.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlanTaskDto(
    val id: String,
    val date: String,
    val type: String,
    val title: LocalizedDto,
    val topicId: String? = null,
    val mockId: String? = null,
    val minutes: Int,
    val reason: LocalizedDto,
    val status: String,
)

@Serializable data class StudyDayDto(val date: String, val tasks: List<PlanTaskDto>)
@Serializable data class PlanAdjustmentDto(val id: String, val summary: LocalizedDto, val date: String)

@Serializable
data class StudyPlanDto(
    val id: String,
    val examId: String,
    val examName: LocalizedDto,
    val examDate: String? = null,
    val days: List<StudyDayDto>,
    val missedTaskCount: Int,
    val adjustments: List<PlanAdjustmentDto> = emptyList(),
)

@Serializable data class TaskStatusDto(val status: String)
@Serializable data class RegeneratePlanDto(val dailyMinutes: Int, val examDate: String? = null)

@Serializable data class ContinueLearningDto(val topicId: String, val topicName: LocalizedDto, val subjectName: LocalizedDto, val progress: Float)
@Serializable data class StreakDto(val current: Int, val longest: Int, val activeDays: List<String>)
@Serializable data class WeakTopicDto(val topicId: String, val name: LocalizedDto, val subjectName: LocalizedDto, val accuracy: Float, val reason: LocalizedDto)
@Serializable data class WeeklyDto(val minutesStudied: Int, val questionsAnswered: Int, val accuracy: Float, val accuracyChange: Float, val dailyMinutes: List<Int>)

@Serializable
data class DashboardDto(
    val greetingName: String,
    val primaryTarget: TargetExamDto? = null,
    val continueLearning: ContinueLearningDto? = null,
    val todayTasks: List<PlanTaskDto>,
    val dailyTargetMinutes: Int,
    val studiedTodayMinutes: Int,
    val streak: StreakDto,
    val syllabusCompletion: Float,
    val overallMastery: Float,
    val weakTopics: List<WeakTopicDto>,
    val revisionDueCount: Int,
    val recentMock: AttemptHistoryDto? = null,
    val recommendedSets: List<PracticeSetDto>,
    val announcements: List<AnnouncementDto>,
    val weekly: WeeklyDto,
)

@Serializable
data class SubjectPerformanceDto(
    val subjectId: String,
    val subjectName: LocalizedDto,
    val accuracy: Float,
    val attempted: Int,
    val mastery: Float,
    val avgSecondsPerQuestion: Float,
)

@Serializable data class TrendPointDto(val date: String, val value: Float)
@Serializable data class RepeatedMistakeDto(val topicId: String, val topicName: LocalizedDto, val pattern: LocalizedDto, val occurrences: Int)

@Serializable
data class AnalyticsDto(
    val overallAccuracy: Float,
    val questionsAttempted: Int,
    val avgSecondsPerQuestion: Float,
    val negativeMarksLost: Double,
    val syllabusCompletion: Float,
    val subjects: List<SubjectPerformanceDto>,
    val accuracyTrend: List<TrendPointDto>,
    val mockScoreTrend: List<TrendPointDto>,
    val repeatedMistakes: List<RepeatedMistakeDto>,
    val revisionRetention: Float,
    val studyDaysLast30: Int,
    val consistency: List<Int>,
    val recommendations: List<RecommendationDto>,
)

@Serializable
data class FlashcardDto(
    val id: String,
    val deckId: String,
    val front: LocalizedDto,
    val back: LocalizedDto,
    val origin: String,
    val topicId: String? = null,
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val easeFactor: Double = 2.5,
    val dueAt: String,
    val lastReviewedAt: String? = null,
)

@Serializable data class DeckDto(val id: String, val name: LocalizedDto, val kind: String, val cardCount: Int, val dueCount: Int)
@Serializable data class CreateFlashcardDto(val clientId: String, val deckId: String, val front: String, val back: String)
@Serializable data class FlashcardReviewDto(val cardId: String, val grade: String, val reviewedAt: String)
@Serializable data class FlashcardBatchDto(val cards: List<FlashcardDto>)

@Serializable
data class NoteDto(
    val id: String,
    val topicId: String? = null,
    val questionId: String? = null,
    val title: String,
    val body: String,
    val updatedAt: String,
)

@Serializable data class AiUsageDto(val usedToday: Int, val dailyLimit: Int)
@Serializable data class TutorContextDto(val questionId: String? = null, val topicId: String? = null, val materialId: String? = null)

@Serializable
data class TutorRequestDto(
    val conversationId: String? = null,
    val intent: String,
    val message: String,
    val context: TutorContextDto,
    val language: String,
)

@Serializable
data class TutorMessageDto(
    val id: String,
    val text: String,
    val sources: List<SourceDto> = emptyList(),
    val createdAt: String,
    val suggestedFollowUps: List<String> = emptyList(),
    val flashcards: List<FlashcardDto> = emptyList(),
)

@Serializable data class TutorResponseDto(val conversationId: String, val message: TutorMessageDto)

/** One queued offline change, replayed by the sync endpoint in order. */
@Serializable data class SyncOperationDto(val id: String, val type: String, val payload: String, val createdAt: String)
@Serializable data class SyncBatchDto(val operations: List<SyncOperationDto>)
@Serializable data class SyncResultDto(val appliedIds: List<String>, val rejectedIds: List<String> = emptyList(), val serverTime: String)
