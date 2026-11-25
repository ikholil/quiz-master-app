package app.prostuti.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import app.prostuti.core.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicProgressDao {
    @Query("SELECT * FROM topic_progress WHERE topicId = :topicId")
    suspend fun get(topicId: String): TopicProgressEntity?

    @Query("SELECT * FROM topic_progress WHERE topicId IN (:ids)")
    suspend fun getAll(ids: List<String>): List<TopicProgressEntity>

    @Upsert suspend fun upsert(entity: TopicProgressEntity)
    @Upsert suspend fun upsertAll(entities: List<TopicProgressEntity>)
}

@Dao
interface OfflineContentDao {
    @Query("SELECT * FROM offline_content ORDER BY downloadedAt DESC")
    fun observeAll(): Flow<List<OfflineContentEntity>>

    @Query("SELECT * FROM offline_content WHERE id = :id")
    suspend fun get(id: String): OfflineContentEntity?

    @Query("SELECT id FROM offline_content WHERE state = 'COMPLETED'")
    fun observeCompletedIds(): Flow<List<String>>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM offline_content WHERE state = 'COMPLETED'")
    fun observeTotalBytes(): Flow<Long>

    @Upsert suspend fun upsert(entity: OfflineContentEntity)

    @Query("UPDATE offline_content SET state = :state, progress = :progress WHERE id = :id")
    suspend fun updateState(id: String, state: String, progress: Float)

    @Query("DELETE FROM offline_content WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface OfflineQuestionDao {
    @Query("SELECT * FROM offline_questions WHERE topicId IN (:topicIds) ORDER BY RANDOM() LIMIT :limit")
    suspend fun byTopics(topicIds: List<String>, limit: Int): List<OfflineQuestionEntity>

    @Query("SELECT * FROM offline_questions ORDER BY RANDOM() LIMIT :limit")
    suspend fun random(limit: Int): List<OfflineQuestionEntity>

    @Query("SELECT * FROM offline_questions WHERE ownerId = :ownerId")
    suspend fun byOwner(ownerId: String): List<OfflineQuestionEntity>

    @Query("SELECT COUNT(*) FROM offline_questions")
    fun observeCount(): Flow<Int>

    @Upsert suspend fun upsertAll(entities: List<OfflineQuestionEntity>)

    @Query("DELETE FROM offline_questions WHERE ownerId = :ownerId")
    suspend fun deleteByOwner(ownerId: String)
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM decks ORDER BY nameEn")
    fun observeDecks(): Flow<List<DeckEntity>>

    @Query("SELECT deckId AS deckId, COUNT(*) AS total, SUM(CASE WHEN dueAt <= :now THEN 1 ELSE 0 END) AS due FROM flashcards GROUP BY deckId")
    fun observeDeckCounts(now: Long): Flow<List<DeckCount>>

    @Query("SELECT * FROM flashcards WHERE dueAt <= :now AND (:deckId IS NULL OR deckId = :deckId) ORDER BY dueAt LIMIT :limit")
    suspend fun due(deckId: String?, now: Long, limit: Int): List<FlashcardEntity>

    @Query("SELECT COUNT(*) FROM flashcards WHERE dueAt <= :now")
    fun observeDueCount(now: Long): Flow<Int>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId")
    suspend fun byDeck(deckId: String): List<FlashcardEntity>

    @Upsert suspend fun upsertDecks(decks: List<DeckEntity>)
    @Upsert suspend fun upsert(card: FlashcardEntity)
    @Upsert suspend fun upsertAll(cards: List<FlashcardEntity>)
}

data class DeckCount(val deckId: String, val total: Int, val due: Int)

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE deleted = 0 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: String): NoteEntity?

    @Upsert suspend fun upsert(note: NoteEntity)
    @Upsert suspend fun upsertAll(notes: List<NoteEntity>)

    @Query("UPDATE notes SET deleted = 1, synced = 0, updatedAt = :at WHERE id = :id")
    suspend fun softDelete(id: String, at: Long)
}

@Dao
interface WrongAnswerDao {
    @Query("SELECT * FROM wrong_answers ORDER BY resolved ASC, lastWrongAt DESC")
    fun observeAll(): Flow<List<WrongAnswerEntity>>

    @Query("SELECT COUNT(*) FROM wrong_answers WHERE resolved = 0")
    fun observeOpenCount(): Flow<Int>

    @Query("SELECT * FROM wrong_answers WHERE questionId = :id")
    suspend fun get(id: String): WrongAnswerEntity?

    @Upsert suspend fun upsert(entity: WrongAnswerEntity)
    @Upsert suspend fun upsertAll(entities: List<WrongAnswerEntity>)

    @Query("UPDATE wrong_answers SET resolved = 1 WHERE questionId = :id")
    suspend fun resolve(id: String)

    @Query("SELECT questionId FROM wrong_answers WHERE resolved = 0 ORDER BY timesWrong DESC LIMIT :limit")
    suspend fun openIds(limit: Int): List<String>

    /** Records a wrong answer, incrementing the counter when the question was already in the notebook. */
    @Transaction
    suspend fun recordWrong(questionId: String, questionJson: String, selected: String, at: Long) {
        val existing = get(questionId)
        upsert(
            WrongAnswerEntity(
                questionId = questionId,
                questionJson = questionJson,
                selectedOptionIds = selected,
                timesWrong = (existing?.timesWrong ?: 0) + 1,
                lastWrongAt = at,
                resolved = false,
            ),
        )
    }
}

@Dao
interface PendingOperationDao {
    @Query("SELECT * FROM pending_operations ORDER BY createdAt LIMIT :limit")
    suspend fun oldest(limit: Int): List<PendingOperationEntity>

    @Query("SELECT COUNT(*) FROM pending_operations")
    fun observeCount(): Flow<Int>

    @Upsert suspend fun enqueue(op: PendingOperationEntity)

    @Query("DELETE FROM pending_operations WHERE id IN (:ids)")
    suspend fun delete(ids: List<String>)

    @Query("UPDATE pending_operations SET attempts = attempts + 1 WHERE id IN (:ids)")
    suspend fun incrementAttempts(ids: List<String>)
}
