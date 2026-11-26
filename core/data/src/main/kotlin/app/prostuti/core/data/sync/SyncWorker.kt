package app.prostuti.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.prostuti.core.common.AppError
import app.prostuti.core.common.AppResult
import app.prostuti.core.database.dao.PendingOperationDao
import app.prostuti.core.datastore.UserPreferencesDataSource
import app.prostuti.core.network.api.RevisionApi
import app.prostuti.core.network.dto.SyncBatchDto
import app.prostuti.core.network.dto.SyncOperationDto
import app.prostuti.core.network.safeApiCall
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Replays the offline outbox to the backend in creation order, in batches of 50.
 * Rejected operations (e.g. a conflicting edit the server refuses) are dropped after 5 attempts so
 * one bad record never blocks the queue.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dao: PendingOperationDao,
    private val api: RevisionApi,
    private val prefs: UserPreferencesDataSource,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        while (true) {
            val batch = dao.oldest(BATCH)
            if (batch.isEmpty()) break
            val body = SyncBatchDto(batch.map { SyncOperationDto(it.id, it.type, it.payloadJson, Instant.ofEpochMilli(it.createdAt).toString()) })
            when (val result = safeApiCall { api.sync(body).data }) {
                is AppResult.Success -> {
                    dao.delete(result.data.appliedIds)
                    val rejected = result.data.rejectedIds
                    if (rejected.isNotEmpty()) {
                        dao.markRejected(rejected, "rejected by server at ${result.data.serverTime}")
                        dao.delete(batch.filter { it.id in rejected && it.attempts + 1 >= MAX_ATTEMPTS }.map { it.id })
                    }
                    if (result.data.appliedIds.isEmpty()) break
                }
                is AppResult.Failure -> return when (result.error) {
                    AppError.SessionExpired -> Result.failure()
                    else -> Result.retry()
                }
            }
        }
        prefs.setLastSyncedAt(System.currentTimeMillis())
        return Result.success()
    }

    companion object {
        const val BATCH = 50
        const val MAX_ATTEMPTS = 5
        const val UNIQUE_NOW = "sync-now"
        const val UNIQUE_PERIODIC = "sync-periodic"
    }
}

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Runs as soon as there is connectivity; coalesces repeated requests. */
    fun requestSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(SyncWorker.UNIQUE_NOW, ExistingWorkPolicy.KEEP, request)
    }

    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).setRequiresBatteryNotLow(true).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(SyncWorker.UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    val isSyncing: Flow<Boolean> = WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkFlow(SyncWorker.UNIQUE_NOW)
        .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING } }
}
