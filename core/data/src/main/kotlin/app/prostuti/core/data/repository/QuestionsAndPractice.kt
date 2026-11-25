package app.prostuti.core.data.repository

import app.prostuti.core.common.AppError
import app.prostuti.core.common.AppResult
import app.prostuti.core.common.asSuccess
import app.prostuti.core.common.onSuccess
import app.prostuti.core.data.mapper.toDto
import app.prostuti.core.data.mapper.toModel
import app.prostuti.core.data.sync.Outbox
import app.prostuti.core.data.sync.SyncOps
import app.prostuti.core.database.dao.OfflineContentDao
import app.prostuti.core.database.dao.OfflineQuestionDao
import app.prostuti.core.database.dao.WrongAnswerDao
import app.prostuti.core.database.entity.WrongAnswerEntity
import app.prostuti.core.domain.exam.NegativeMarking
import app.prostuti.core.domain.practice.PracticeEvaluator
import app.prostuti.core.domain.repository.PracticeRepository
import app.prostuti.core.domain.repository.QuestionRepository
import app.prostuti.core.model.*
import app.prostuti.core.network.NetworkJson
import app.prostuti.core.network.api.PracticeApi
import app.prostuti.core.network.api.QuestionApi
import app.prostuti.core.network.dto.*
import app.prostuti.core.network.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Converts a domain question back to its DTO for local persistence (wrong-answer notebook). */
internal fun Question.toDto(): QuestionDto = QuestionDto(
    id = id,
    type = type.name,
    stem = stem.toDto(),
    options = options.map { OptionDto(it.id, it.text.toDto(), it.imageUrl) },
    passage = passage?.let { PassageDto(it.id, it.title?.toDto(), it.text.toDto()) },
    imageUrl = imageUrl,
    formula = formula,
    difficulty = difficulty.name,
    provenance = provenance.name,
    topicId = topicId,
    topicName = topicName.toDto(),
    subjectName = subjectName.toDto(),
    examReferences = examReferences.map { ExamRefDto(it.examName.toDto(), it.year, it.stage) },
    sources = sources.map { SourceDto(it.title, it.publisher, it.url, it.publishedAt?.toString()) },
    solution = solution?.let { s ->
        SolutionDto(s.correctOptionIds.toList(), s.explanation.toDto(), s.optionExplanations.mapValues { it.value.toDto() }, s.modelAnswer?.toDto())
    },
    isBookmarked = isBookmarked,
    note = note,
)

private val idList = ListSerializer(String.serializer())

@Singleton
class LiveQuestionRepository @Inject constructor(
    private val api: QuestionApi,
    private val wrongDao: WrongAnswerDao,
    private val outbox: Outbox,
) : QuestionRepository {

    override suspend fun questions(filter: QuestionFilter, cursor: String?) = safeApiCall {
        val env = api.questions(
            filter.query.ifBlank { null }, filter.examId, filter.subjectId, filter.topicId,
            filter.difficulty?.name, filter.type?.name, filter.provenance?.name,
            filter.previousYearOnly.takeIf { it }, filter.bookmarkedOnly.takeIf { it }, cursor,
        )
        Page(env.data.map { it.toModel() }, env.meta?.nextCursor)
    }

    override suspend fun question(questionId: String) = safeApiCall { api.question(questionId).data.toModel() }
    override suspend fun similar(questionId: String) = safeApiCall { api.similar(questionId).data.map { it.toModel() } }
    override suspend fun setBookmarked(questionId: String, bookmarked: Boolean) = safeApiCall {
        if (bookmarked) api.bookmark(questionId) else api.unbookmark(questionId); Unit
    }
    override suspend fun saveNote(questionId: String, note: String) = safeApiCall { api.saveNote(questionId, NoteBodyDto(note)); Unit }
    override suspend fun report(questionId: String, reason: QuestionReportReason, details: String) =
        safeApiCall { api.report(questionId, ReportDto(reason.name, details)); Unit }

    /** Local-first notebook; refreshed from the server so mistakes from other devices appear too. */
    override fun wrongAnswers(): Flow<List<WrongAnswerEntry>> = wrongDao.observeAll().map { rows ->
        rows.mapNotNull { row ->
            runCatching {
                WrongAnswerEntry(
                    NetworkJson.decodeFromString(QuestionDto.serializer(), row.questionJson).toModel(),
                    NetworkJson.decodeFromString(idList, row.selectedOptionIds).toSet(),
                    row.timesWrong, Instant.ofEpochMilli(row.lastWrongAt), row.resolved,
                )
            }.getOrNull()
        }
    }

    suspend fun refreshWrongAnswers() = safeApiCall { api.wrongAnswers().data }.onSuccess { list ->
        wrongDao.upsertAll(list.map {
            WrongAnswerEntity(
                it.question.id, NetworkJson.encodeToString(QuestionDto.serializer(), it.question),
                NetworkJson.encodeToString(idList, it.selectedOptionIds), it.timesWrong,
                Instant.parse(it.lastWrongAt).toEpochMilli(), it.resolved,
            )
        })
    }

    override suspend fun markWrongAnswerResolved(questionId: String): AppResult<Unit> {
        wrongDao.resolve(questionId)
        outbox.enqueue(SyncOps.WRONG_ANSWER_RESOLVE, NetworkJson.encodeToString(idList, listOf(questionId)))
        return Unit.asSuccess()
    }

    override suspend fun collections() = safeApiCall { api.collections().data.map { it.toModel() } }
    override suspend fun createCollection(name: String) = safeApiCall { api.createCollection(CreateCollectionDto(name)).data.toModel() }
    override suspend fun addToCollection(collectionId: String, questionId: String) =
        safeApiCall { api.addToCollection(collectionId, AddToCollectionDto(questionId)); Unit }
}

/**
 * Online: the backend picks questions (adaptive) and scores. Offline: questions come from downloaded
 * bundles, scoring runs on-device via [PracticeEvaluator], and the result is queued for sync.
 */
@Singleton
class LivePracticeRepository @Inject constructor(
    private val api: PracticeApi,
    private val offlineQuestions: OfflineQuestionDao,
    private val offlineContent: OfflineContentDao,
    private val wrongDao: WrongAnswerDao,
    private val outbox: Outbox,
) : PracticeRepository {

    override suspend fun start(config: PracticeConfig): AppResult<PracticeSession> {
        if (!config.offlineOnly) {
            val online = safeApiCall { api.start(config.toDto()).data.toModel() }
            if (online is AppResult.Success || (online as AppResult.Failure).error.let { it !is AppError.NoConnection && it !is AppError.Timeout }) {
                return online
            }
        }
        return startOffline(config)
    }

    private suspend fun startOffline(config: PracticeConfig): AppResult<PracticeSession> {
        val rows = when (config.mode) {
            PracticeMode.WRONG_ANSWERS -> {
                val ids = wrongDao.openIds(config.questionCount)
                return ids.mapNotNull { id -> wrongDao.get(id) }
                    .map { NetworkJson.decodeFromString(QuestionDto.serializer(), it.questionJson).toModel() }
                    .takeIf { it.isNotEmpty() }
                    ?.let { AppResult.Success(PracticeSession("offline-${UUID.randomUUID()}", config, it, isOffline = true)) }
                    ?: AppResult.Failure(AppError.NoConnection)
            }
            else -> if (config.topicIds.isNotEmpty()) offlineQuestions.byTopics(config.topicIds, config.questionCount)
            else offlineQuestions.random(config.questionCount)
        }
        val questions = rows.map { NetworkJson.decodeFromString(QuestionDto.serializer(), it.payloadJson).toModel() }
            .filter { it.solution != null && config.difficulty?.let { d -> it.difficulty == d } != false }
        if (questions.isEmpty()) return AppResult.Failure(AppError.NoConnection)
        return AppResult.Success(PracticeSession("offline-${UUID.randomUUID()}", config, questions, isOffline = true))
    }

    override suspend fun submit(session: PracticeSession, answers: List<AnswerRecord>): AppResult<PracticeResult> {
        recordMistakes(session, answers)
        if (!session.isOffline) {
            val online = safeApiCall { api.submit(session.id, SubmitPracticeDto(answers.map { it.toDto() })).data.toModel() }
            if (online is AppResult.Success) return online
        }
        val local = PracticeEvaluator.evaluate(session, answers)
        outbox.enqueue(
            SyncOps.PRACTICE_RESULT,
            NetworkJson.encodeToString(SubmitPracticeDto.serializer(), SubmitPracticeDto(answers.map { it.toDto() }, clientSessionId = session.id)),
        )
        return local.asSuccess()
    }

    private suspend fun recordMistakes(session: PracticeSession, answers: List<AnswerRecord>) {
        val byId = answers.associateBy { it.questionId }
        val now = System.currentTimeMillis()
        session.questions.forEach { q ->
            val selected = byId[q.id]?.selectedOptionIds.orEmpty()
            val correct = q.solution?.correctOptionIds ?: return@forEach
            if (selected.isNotEmpty() && !NegativeMarking.isAnswerCorrect(selected, correct)) {
                wrongDao.recordWrong(q.id, NetworkJson.encodeToString(QuestionDto.serializer(), q.toDto()), NetworkJson.encodeToString(idList, selected.toList()), now)
            }
        }
    }

    override suspend fun recommendedSets(): AppResult<List<PracticeSetSummary>> {
        val downloaded = offlineContent.observeCompletedIds().first().toSet()
        return safeApiCall { api.recommendedSets().data.map { it.toModel(it.id in downloaded) } }
    }

    override suspend fun dailyChallenge() = safeApiCall { api.dailyChallenge().data.toModel() }
}
