package app.prostuti.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per topic — never per (exam, topic). Shared topics therefore keep a single
 * learning record no matter how many target exams include them.
 */
@Entity(tableName = "topic_progress")
data class TopicProgressEntity(
    @PrimaryKey val topicId: String,
    val completed: Boolean,
    val mastery: Float,
    val bookmarked: Boolean,
    val updatedAt: Long,
    val synced: Boolean,
)

/** Downloaded offline content (topic bundles, question sets, decks) as serialized API payloads. */
@Entity(tableName = "offline_content", indices = [Index("kind")])
data class OfflineContentEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val titleEn: String,
    val titleBn: String?,
    val payloadJson: String,
    val sizeBytes: Long,
    val state: String,
    val progress: Float,
    val downloadedAt: Long?,
)

/** Questions available offline; [ownerId] is the bundle (topic or set) they came from. */
@Entity(tableName = "offline_questions", indices = [Index("topicId"), Index("ownerId")])
data class OfflineQuestionEntity(
    @PrimaryKey val id: String,
    val topicId: String,
    val ownerId: String,
    val payloadJson: String,
)

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey val id: String,
    val nameEn: String,
    val nameBn: String?,
    val kind: String,
    val offline: Boolean,
)

@Entity(tableName = "flashcards", indices = [Index("deckId"), Index("dueAt")])
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val deckId: String,
    val frontEn: String,
    val frontBn: String?,
    val backEn: String,
    val backBn: String?,
    val origin: String,
    val topicId: String?,
    val repetitions: Int,
    val intervalDays: Int,
    val easeFactor: Double,
    val dueAt: Long,
    val lastReviewedAt: Long?,
)

@Entity(tableName = "notes", indices = [Index("topicId"), Index("questionId")])
data class NoteEntity(
    @PrimaryKey val id: String,
    val topicId: String?,
    val questionId: String?,
    val title: String,
    val body: String,
    val updatedAt: Long,
    val synced: Boolean,
    val deleted: Boolean,
)

@Entity(tableName = "wrong_answers")
data class WrongAnswerEntity(
    @PrimaryKey val questionId: String,
    val questionJson: String,
    val selectedOptionIds: String,
    val timesWrong: Int,
    val lastWrongAt: Long,
    val resolved: Boolean,
)

/** Outbox of changes made offline, replayed in order by the sync worker. */
@Entity(tableName = "pending_operations", indices = [Index("createdAt")])
data class PendingOperationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val payloadJson: String,
    val createdAt: Long,
    val attempts: Int,
)
