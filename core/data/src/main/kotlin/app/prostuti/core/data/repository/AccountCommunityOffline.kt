package app.prostuti.core.data.repository

import android.content.Context
import android.os.StatFs
import app.prostuti.core.common.AppResult
import app.prostuti.core.common.asSuccess
import app.prostuti.core.common.onSuccess
import app.prostuti.core.data.mapper.toDto
import app.prostuti.core.data.mapper.toModel
import app.prostuti.core.data.sync.DownloadScheduler
import app.prostuti.core.data.sync.SyncScheduler
import app.prostuti.core.database.dao.OfflineContentDao
import app.prostuti.core.database.dao.OfflineQuestionDao
import app.prostuti.core.database.dao.PendingOperationDao
import app.prostuti.core.database.entity.OfflineContentEntity
import app.prostuti.core.datastore.UserPreferencesDataSource
import app.prostuti.core.domain.repository.AccountRepository
import app.prostuti.core.domain.repository.CommunityRepository
import app.prostuti.core.domain.repository.OfflineRepository
import app.prostuti.core.model.*
import app.prostuti.core.network.api.AccountApi
import app.prostuti.core.network.api.CommunityApi
import app.prostuti.core.network.api.LearningApi
import app.prostuti.core.network.dto.*
import app.prostuti.core.network.safeApiCall
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveAccountRepository @Inject constructor(private val api: AccountApi) : AccountRepository {
    private val status = MutableStateFlow(SubscriptionStatus.Free)
    override val subscription: Flow<SubscriptionStatus> = status

    override suspend fun refreshSubscription() = safeApiCall { api.subscription().data.toModel() }.onSuccess { status.value = it }
    override suspend fun plans() = safeApiCall { api.plans().data.map { it.toModel() } }
    override suspend fun checkout(planId: String) = safeApiCall { api.checkout(CheckoutRequestDto(planId)).data.checkoutUrl }
    override suspend fun privacy() = safeApiCall { api.privacy().data.toModel() }
    override suspend fun updatePrivacy(settings: PrivacySettings) = safeApiCall { api.updatePrivacy(settings.toDto()).data.toModel() }
    override suspend fun requestDataExport() = safeApiCall { api.requestExport().data.toModel() }
    override suspend fun requestAccountDeletion(reason: String) = safeApiCall { api.requestDeletion(DeletionRequestDto(reason)); Unit }
    override suspend fun achievements() = safeApiCall { api.achievements().data.map { it.toModel() } }
    override suspend fun tickets() = safeApiCall { api.tickets().data.map { it.toModel() } }
    override suspend fun createTicket(subject: String, message: String) = safeApiCall { api.createTicket(CreateTicketDto(subject, message)).data.toModel() }
    override suspend fun notifications() = safeApiCall { api.notifications().data.map { it.toModel() } }
    override suspend fun registerPushToken(token: String) = safeApiCall { api.registerDevice(DeviceDto(token)); Unit }
}

@Singleton
class LiveCommunityRepository @Inject constructor(private val api: CommunityApi) : CommunityRepository {
    override suspend fun groups() = safeApiCall { api.groups().data.map { it.toModel() } }
    override suspend fun joinGroup(groupId: String) = safeApiCall { api.join(groupId); Unit }
    override suspend fun leaveGroup(groupId: String) = safeApiCall { api.leave(groupId); Unit }
    override suspend fun discussions(examShortName: String?, cursor: String?) = safeApiCall {
        val env = api.discussions(examShortName, cursor)
        Page(env.data.map { it.toModel() }, env.meta?.nextCursor)
    }
    override suspend fun replies(discussionId: String) = safeApiCall { api.replies(discussionId).data.map { it.toModel() } }
    override suspend fun postDiscussion(title: String, body: String, questionId: String?) =
        safeApiCall { api.post(PostDiscussionDto(title, body, questionId)).data.toModel() }
    override suspend fun reply(discussionId: String, body: String) = safeApiCall { api.reply(discussionId, PostReplyDto(body)).data.toModel() }
    override suspend fun report(contentId: String, reason: String) = safeApiCall { api.report(contentId, ContentReportDto(reason)); Unit }
    override suspend fun leaderboard(groupId: String) = safeApiCall { api.leaderboard(groupId).data.toModel() }
}

@Singleton
class LiveOfflineRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentDao: OfflineContentDao,
    private val questionDao: OfflineQuestionDao,
    private val pendingDao: PendingOperationDao,
    private val learningApi: LearningApi,
    private val downloads: DownloadScheduler,
    private val sync: SyncScheduler,
    private val prefs: UserPreferencesDataSource,
) : OfflineRepository {

    override fun downloads(): Flow<List<DownloadItem>> = contentDao.observeAll().map { rows ->
        rows.map {
            DownloadItem(
                it.id, runCatching { DownloadKind.valueOf(it.kind) }.getOrDefault(DownloadKind.MATERIAL),
                LocalizedText(it.titleEn, it.titleBn), it.sizeBytes,
                runCatching { DownloadState.valueOf(it.state) }.getOrDefault(DownloadState.FAILED),
                it.progress, it.downloadedAt?.let(Instant::ofEpochMilli),
            )
        }
    }

    override fun storage(): Flow<StorageUsage> = contentDao.observeTotalBytes().map { downloaded ->
        val stat = StatFs(context.filesDir.absolutePath)
        StorageUsage(downloaded, dirSize(context.cacheDir), stat.availableBytes)
    }

    override fun syncStatus(): Flow<SyncStatus> = combine(pendingDao.observeCount(), sync.isSyncing, prefs.preferences) { pending, syncing, p ->
        SyncStatus(pending, p.lastSyncedAtMillis?.let(Instant::ofEpochMilli), syncing)
    }

    override suspend fun downloadTopic(topicId: String): AppResult<Unit> {
        val title = safeApiCall { learningApi.topic(topicId).data.summary.name }
        val name = (title as? AppResult.Success)?.data ?: LocalizedDto(topicId)
        return queue(topicId, DownloadKind.MATERIAL, name)
    }

    override suspend fun downloadQuestionSet(setId: String) = queue(setId, DownloadKind.QUESTION_SET, LocalizedDto("Question set", "প্রশ্ন সেট"))
    override suspend fun downloadDeck(deckId: String) = queue(deckId, DownloadKind.FLASHCARD_DECK, LocalizedDto("Flashcards", "ফ্ল্যাশকার্ড"))

    private suspend fun queue(id: String, kind: DownloadKind, title: LocalizedDto): AppResult<Unit> {
        contentDao.upsert(OfflineContentEntity(id, kind.name, title.en, title.bn, "", 0, DownloadState.QUEUED.name, 0f, null))
        downloads.enqueue(id, kind, prefs.preferences.first().downloadOnWifiOnly)
        return Unit.asSuccess()
    }

    override suspend fun remove(downloadId: String) {
        downloads.cancel(downloadId)
        questionDao.deleteByOwner(downloadId)
        contentDao.delete(downloadId)
    }

    override suspend fun clearCache() {
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    override fun requestSync() = sync.requestSync()

    private fun dirSize(dir: File): Long = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
