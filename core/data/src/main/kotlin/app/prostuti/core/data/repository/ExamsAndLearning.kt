package app.prostuti.core.data.repository

import app.prostuti.core.common.AppError
import app.prostuti.core.common.AppResult
import app.prostuti.core.common.asSuccess
import app.prostuti.core.data.mapper.toDto
import app.prostuti.core.data.mapper.toModel
import app.prostuti.core.data.sync.Outbox
import app.prostuti.core.data.sync.SyncOps
import app.prostuti.core.database.dao.OfflineContentDao
import app.prostuti.core.database.dao.TopicProgressDao
import app.prostuti.core.database.entity.TopicProgressEntity
import app.prostuti.core.domain.repository.DashboardRepository
import app.prostuti.core.domain.repository.ExamRepository
import app.prostuti.core.domain.repository.LearningRepository
import app.prostuti.core.model.*
import app.prostuti.core.network.NetworkJson
import app.prostuti.core.network.api.ExamApi
import app.prostuti.core.network.api.LearningApi
import app.prostuti.core.network.api.ProfileApi
import app.prostuti.core.network.dto.NoteBodyDto
import app.prostuti.core.network.dto.TargetsDto
import app.prostuti.core.network.dto.TopicBundleDto
import app.prostuti.core.network.dto.TopicProgressDto
import app.prostuti.core.network.safeApiCall
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveDashboardRepository @Inject constructor(private val api: ProfileApi) : DashboardRepository {
    override suspend fun dashboard() = safeApiCall { api.dashboard().data.toModel() }
}

@Singleton
class LiveExamRepository @Inject constructor(private val api: ExamApi) : ExamRepository {
    override suspend fun exams(filter: ExamFilter, cursor: String?) = safeApiCall {
        val env = api.exams(filter.query.ifBlank { null }, filter.category?.name, filter.bookmarkedOnly.takeIf { it }, cursor)
        Page(env.data.map { it.toModel() }, env.meta?.nextCursor)
    }

    override suspend fun exam(examId: String) = safeApiCall { api.exam(examId).data.toModel() }

    override suspend fun setBookmarked(examId: String, bookmarked: Boolean) = safeApiCall {
        if (bookmarked) api.bookmark(examId) else api.unbookmark(examId); Unit
    }

    override suspend fun compare(examIds: List<String>) = safeApiCall { api.compare(examIds.joinToString(",")).data.toModel() }
    override suspend fun setTargets(targets: List<TargetExam>) = safeApiCall { api.setTargets(TargetsDto(targets.map { it.toDto() })); Unit }
    override suspend fun announcements() = safeApiCall { api.announcements().data.map { it.toModel() } }
}

@Serializable internal data class TopicProgressOp(val topicId: String, val completed: Boolean, val updatedAt: String)
@Serializable internal data class TopicBookmarkOp(val topicId: String, val bookmarked: Boolean)

/**
 * Network-first for browsing, with local overlays:
 * - completion toggled offline is stored per topic id and wins until synced;
 * - downloaded topic bundles are served when the network is unavailable.
 */
@Singleton
class LiveLearningRepository @Inject constructor(
    private val api: LearningApi,
    private val progressDao: TopicProgressDao,
    private val offlineDao: OfflineContentDao,
    private val outbox: Outbox,
) : LearningRepository {

    override suspend fun subjects(examId: String) = safeApiCall { api.subjects(examId).data.map { it.toModel() } }

    override suspend fun chapters(subjectId: String): AppResult<List<Chapter>> {
        val offlineIds = offlineDao.observeCompletedIds().first().toSet()
        return when (val r = safeApiCall { api.chapters(subjectId).data }) {
            is AppResult.Success -> {
                val chapters = r.data.map { it.toModel(offlineIds) }
                overlayProgress(chapters.flatMap { it.topics }).let { overlay ->
                    chapters.map { c -> c.copy(topics = c.topics.map { overlay[it.id] ?: it }) }.asSuccess()
                }
            }
            is AppResult.Failure -> r
        }
    }

    private suspend fun overlayProgress(topics: List<TopicSummary>): Map<String, TopicSummary> {
        val local = progressDao.getAll(topics.map { it.id }).filter { !it.synced }.associateBy { it.topicId }
        return topics.associate { t -> t.id to (local[t.id]?.let { t.copy(isCompleted = it.completed) } ?: t) }
    }

    override suspend fun topic(topicId: String): AppResult<TopicDetail> {
        val online = safeApiCall { api.topic(topicId).data }
        val bundle = offlineBundle(topicId)
        return when {
            online is AppResult.Success -> {
                val detail = online.data.toModel(offline = bundle != null)
                val local = progressDao.get(topicId)?.takeIf { !it.synced }
                AppResult.Success(local?.let { detail.copy(summary = detail.summary.copy(isCompleted = it.completed)) } ?: detail)
            }
            bundle != null -> AppResult.Success(bundle.topic.toModel(offline = true))
            else -> online as AppResult.Failure
        }
    }

    override suspend fun material(materialId: String): AppResult<LearningMaterial> {
        val online = safeApiCall { api.material(materialId).data.toModel() }
        if (online is AppResult.Success) return online
        // Search downloaded bundles for this material.
        offlineDao.observeAll().first().forEach { entity ->
            val bundle = runCatching { NetworkJson.decodeFromString(TopicBundleDto.serializer(), entity.payloadJson) }.getOrNull()
            bundle?.materials?.firstOrNull { it.summary.id == materialId }?.let { return AppResult.Success(it.toModel(downloaded = true)) }
        }
        return online
    }

    private suspend fun offlineBundle(topicId: String): TopicBundleDto? =
        offlineDao.get(topicId)?.takeIf { it.state == DownloadState.COMPLETED.name }?.let {
            runCatching { NetworkJson.decodeFromString(TopicBundleDto.serializer(), it.payloadJson) }.getOrNull()
        }

    override suspend fun setTopicCompleted(topicId: String, completed: Boolean): AppResult<Unit> {
        val now = Instant.now()
        val existing = progressDao.get(topicId)
        val result = safeApiCall { api.setProgress(topicId, TopicProgressDto(completed, now.toString())); Unit }
        val synced = result is AppResult.Success
        progressDao.upsert(
            TopicProgressEntity(topicId, completed, existing?.mastery ?: 0f, existing?.bookmarked ?: false, now.toEpochMilli(), synced),
        )
        if (!synced) {
            if ((result as AppResult.Failure).error !is AppError.NoConnection && result.error !is AppError.Timeout) return result
            outbox.enqueue(SyncOps.TOPIC_PROGRESS, NetworkJson.encodeToString(TopicProgressOp.serializer(), TopicProgressOp(topicId, completed, now.toString())))
        }
        return Unit.asSuccess()
    }

    override suspend fun setTopicBookmarked(topicId: String, bookmarked: Boolean) = safeApiCall {
        if (bookmarked) api.bookmark(topicId) else api.unbookmark(topicId); Unit
    }

    override suspend fun saveTopicNote(topicId: String, note: String) = safeApiCall { api.saveNote(topicId, NoteBodyDto(note)); Unit }
}
