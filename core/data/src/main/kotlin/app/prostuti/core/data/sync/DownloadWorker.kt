package app.prostuti.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.prostuti.core.common.AppResult
import app.prostuti.core.database.dao.FlashcardDao
import app.prostuti.core.database.dao.OfflineContentDao
import app.prostuti.core.database.dao.OfflineQuestionDao
import app.prostuti.core.database.entity.DeckEntity
import app.prostuti.core.database.entity.OfflineQuestionEntity
import app.prostuti.core.data.repository.toEntity
import app.prostuti.core.model.DownloadKind
import app.prostuti.core.model.DownloadState
import app.prostuti.core.network.NetworkJson
import app.prostuti.core.network.api.LearningApi
import app.prostuti.core.network.api.PracticeApi
import app.prostuti.core.network.api.RevisionApi
import app.prostuti.core.network.dto.QuestionDto
import app.prostuti.core.network.dto.QuestionSetBundleDto
import app.prostuti.core.network.dto.TopicBundleDto
import app.prostuti.core.network.safeApiCall
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Downloads an offline bundle and stores it in Room. Respects the user's Wi-Fi-only setting. */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val contentDao: OfflineContentDao,
    private val questionDao: OfflineQuestionDao,
    private val flashcardDao: FlashcardDao,
    private val learningApi: LearningApi,
    private val practiceApi: PracticeApi,
    private val revisionApi: RevisionApi,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_ID) ?: return Result.failure()
        val kind = DownloadKind.valueOf(inputData.getString(KEY_KIND) ?: return Result.failure())
        contentDao.updateState(id, DownloadState.DOWNLOADING.name, 0.1f)
        val outcome: AppResult<Pair<String, List<QuestionDto>>> = when (kind) {
            DownloadKind.MATERIAL -> safeApiCall {
                val bundle = learningApi.topicBundle(id).data
                NetworkJson.encodeToString(TopicBundleDto.serializer(), bundle) to bundle.questions
            }
            DownloadKind.QUESTION_SET -> safeApiCall {
                val bundle = practiceApi.setBundle(id).data
                NetworkJson.encodeToString(QuestionSetBundleDto.serializer(), bundle) to bundle.questions
            }
            DownloadKind.FLASHCARD_DECK -> safeApiCall {
                val cards = revisionApi.flashcards(id, null).data
                flashcardDao.upsertAll(cards.map { it.toEntity() })
                "" to emptyList()
            }
        }
        return when (outcome) {
            is AppResult.Success -> {
                val (payload, questions) = outcome.data
                contentDao.updateState(id, DownloadState.DOWNLOADING.name, 0.7f)
                questionDao.deleteByOwner(id)
                questionDao.upsertAll(questions.map {
                    OfflineQuestionEntity(it.id, it.topicId, id, NetworkJson.encodeToString(QuestionDto.serializer(), it))
                })
                val existing = contentDao.get(id)
                if (existing != null) {
                    contentDao.upsert(existing.copy(
                        payloadJson = payload,
                        sizeBytes = payload.length.toLong() + questions.sumOf { 600L },
                        state = DownloadState.COMPLETED.name,
                        progress = 1f,
                        downloadedAt = System.currentTimeMillis(),
                    ))
                }
                if (kind == DownloadKind.FLASHCARD_DECK) {
                    existing?.let { flashcardDao.upsertDecks(listOf(DeckEntity(id, it.titleEn, it.titleBn, "GENERAL", true))) }
                }
                Result.success()
            }
            is AppResult.Failure -> {
                if (runAttemptCount < 3) Result.retry() else {
                    contentDao.updateState(id, DownloadState.FAILED.name, 0f)
                    Result.failure()
                }
            }
        }
    }

    companion object {
        const val KEY_ID = "id"
        const val KEY_KIND = "kind"
    }
}

@Singleton
class DownloadScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    fun enqueue(id: String, kind: DownloadKind, wifiOnly: Boolean) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(DownloadWorker.KEY_ID to id, DownloadWorker.KEY_KIND to kind.name))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                    .setRequiresStorageNotLow(true)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("download-$id", ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(id: String) = WorkManager.getInstance(context).cancelUniqueWork("download-$id")
}
