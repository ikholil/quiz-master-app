package app.prostuti.core.database

import android.content.Context
import androidx.room.Room
import app.prostuti.core.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): ProstutiDatabase =
        Room.databaseBuilder(context, ProstutiDatabase::class.java, "prostuti.db")
            .addMigrations(*Migrations.ALL)
            .build()

    @Provides fun topicProgressDao(db: ProstutiDatabase) = db.topicProgressDao()
    @Provides fun offlineContentDao(db: ProstutiDatabase) = db.offlineContentDao()
    @Provides fun offlineQuestionDao(db: ProstutiDatabase) = db.offlineQuestionDao()
    @Provides fun flashcardDao(db: ProstutiDatabase) = db.flashcardDao()
    @Provides fun noteDao(db: ProstutiDatabase) = db.noteDao()
    @Provides fun wrongAnswerDao(db: ProstutiDatabase) = db.wrongAnswerDao()
    @Provides fun pendingOperationDao(db: ProstutiDatabase) = db.pendingOperationDao()
}
