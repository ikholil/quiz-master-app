package app.prostuti.core.model

import java.time.Instant

enum class DownloadKind { MATERIAL, QUESTION_SET, FLASHCARD_DECK }
enum class DownloadState { QUEUED, DOWNLOADING, COMPLETED, FAILED }

data class DownloadItem(
    val id: String,
    val kind: DownloadKind,
    val title: LocalizedText,
    val sizeBytes: Long,
    val state: DownloadState,
    val progress: Float,
    val downloadedAt: Instant?,
)

data class StorageUsage(
    val downloadsBytes: Long,
    val cacheBytes: Long,
    val freeDeviceBytes: Long,
)

data class SyncStatus(
    val pendingChanges: Int,
    val lastSyncedAt: Instant?,
    val isSyncing: Boolean,
)
