package app.prostuti.core.data.repository

import app.prostuti.core.common.AppResult
import app.prostuti.core.data.mapper.toModel
import app.prostuti.core.data.mapper.toSavedAnswers
import app.prostuti.core.domain.repository.AnalyticsRepository
import app.prostuti.core.domain.repository.MockExamRepository
import app.prostuti.core.domain.repository.PlannerRepository
import app.prostuti.core.domain.repository.TutorRepository
import app.prostuti.core.model.*
import app.prostuti.core.network.api.MockExamApi
import app.prostuti.core.network.api.PlannerApi
import app.prostuti.core.network.api.ProfileApi
import app.prostuti.core.network.api.RevisionApi
import app.prostuti.core.network.api.TutorApi
import app.prostuti.core.network.dto.*
import app.prostuti.core.network.safeApiCall
import app.prostuti.core.data.mapper.toDto
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Mock exams are online-only: the backend owns timing, scoring and submission rules. */
@Singleton
class LiveMockExamRepository @Inject constructor(private val api: MockExamApi) : MockExamRepository {
    override suspend fun mocks(examId: String?) = safeApiCall { api.mocks(examId).data.map { it.toModel() } }
    override suspend fun mock(mockId: String) = safeApiCall { api.mock(mockId).data.toModel() }
    override suspend fun startAttempt(mockId: String) = safeApiCall { api.startAttempt(mockId).data.toModel() }
    override suspend fun resumeAttempt(attemptId: String) = safeApiCall { api.attempt(attemptId).data.toModel() }
    override suspend fun saveAnswers(attemptId: String, answers: Map<String, Set<String>>, markedForReview: Set<String>) =
        safeApiCall { api.saveAnswers(attemptId, SaveAnswersDto(answers.toSavedAnswers(), markedForReview.toList())); Unit }
    override suspend fun submit(attemptId: String, answers: Map<String, Set<String>>) =
        safeApiCall { api.submit(attemptId, SubmitAttemptDto(answers.toSavedAnswers())).data.toModel() }
    override suspend fun result(attemptId: String) = safeApiCall { api.result(attemptId).data.toModel() }
    override suspend fun history() = safeApiCall { api.history().data.map { it.toModel() } }
}

@Singleton
class LiveAnalyticsRepository @Inject constructor(private val api: ProfileApi) : AnalyticsRepository {
    override suspend fun analytics(range: AnalyticsRange) = safeApiCall { api.analytics(range.name.lowercase()).data.toModel() }
}

@Singleton
class LiveTutorRepository @Inject constructor(
    private val api: TutorApi,
    private val revisionApi: RevisionApi,
) : TutorRepository {
    override suspend fun usage() = safeApiCall { api.usage().data.let { AiUsage(it.usedToday, it.dailyLimit) } }

    override suspend fun ask(
        conversationId: String?,
        intent: TutorIntent,
        message: String,
        context: TutorContext,
        language: AppLanguage,
    ): AppResult<Pair<String, ChatMessage>> = safeApiCall {
        val response = api.ask(
            TutorRequestDto(conversationId, intent.name, message, TutorContextDto(context.questionId, context.topicId, context.materialId), language.tag),
        ).data
        response.conversationId to response.message.toModel()
    }

    override suspend fun saveFlashcards(cards: List<Flashcard>) =
        safeApiCall { revisionApi.saveBatch(FlashcardBatchDto(cards.map { it.toDto() })); Unit }
}

@Singleton
class LivePlannerRepository @Inject constructor(private val api: PlannerApi) : PlannerRepository {
    override suspend fun plan(examId: String?) = safeApiCall { api.plan(examId).data.toModel() }
    override suspend fun setTaskStatus(taskId: String, status: PlanTaskStatus) = safeApiCall { api.setStatus(taskId, TaskStatusDto(status.name)); Unit }
    override suspend fun recoverMissedTasks() = safeApiCall { api.recover().data.toModel() }
    override suspend fun regenerate(dailyMinutes: Int, examDate: LocalDate?) =
        safeApiCall { api.regenerate(RegeneratePlanDto(dailyMinutes, examDate?.toString())).data.toModel() }
}
