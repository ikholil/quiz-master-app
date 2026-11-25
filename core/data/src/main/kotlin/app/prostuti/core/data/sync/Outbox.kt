package app.prostuti.core.data.sync

import app.prostuti.core.database.dao.PendingOperationDao
import app.prostuti.core.database.entity.PendingOperationEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Operation types understood by the backend's POST /v1/sync endpoint. */
object SyncOps {
    const val TOPIC_PROGRESS = "topic.progress"
    const val TOPIC_BOOKMARK = "topic.bookmark"
    const val FLASHCARD_REVIEW = "flashcard.review"
    const val FLASHCARD_CREATE = "flashcard.create"
    const val NOTE_UPSERT = "note.upsert"
    const val NOTE_DELETE = "note.delete"
    const val PRACTICE_RESULT = "practice.offline_result"
    const val WRONG_ANSWER_RESOLVE = "wrong_answer.resolve"
}

@Singleton
class Outbox @Inject constructor(
    private val dao: PendingOperationDao,
    private val scheduler: SyncScheduler,
) {
    suspend fun enqueue(type: String, payloadJson: String) {
        dao.enqueue(PendingOperationEntity(UUID.randomUUID().toString(), type, payloadJson, System.currentTimeMillis(), 0))
        scheduler.requestSync()
    }
}
