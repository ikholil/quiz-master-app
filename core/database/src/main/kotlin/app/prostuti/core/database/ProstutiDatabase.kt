package app.prostuti.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import app.prostuti.core.database.dao.*
import app.prostuti.core.database.entity.*

@Database(
    entities = [
        TopicProgressEntity::class,
        OfflineContentEntity::class,
        OfflineQuestionEntity::class,
        DeckEntity::class,
        FlashcardEntity::class,
        NoteEntity::class,
        WrongAnswerEntity::class,
        PendingOperationEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class ProstutiDatabase : RoomDatabase() {
    abstract fun topicProgressDao(): TopicProgressDao
    abstract fun offlineContentDao(): OfflineContentDao
    abstract fun offlineQuestionDao(): OfflineQuestionDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun noteDao(): NoteDao
    abstract fun wrongAnswerDao(): WrongAnswerDao
    abstract fun pendingOperationDao(): PendingOperationDao
}
