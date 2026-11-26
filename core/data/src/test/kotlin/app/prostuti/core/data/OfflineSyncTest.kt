package app.prostuti.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import app.prostuti.core.common.AppResult
import app.prostuti.core.data.repository.LivePracticeRepository
import app.prostuti.core.data.sync.Outbox
import app.prostuti.core.data.sync.SyncOps
import app.prostuti.core.data.sync.SyncScheduler
import app.prostuti.core.data.sync.SyncWorker
import app.prostuti.core.database.ProstutiDatabase
import app.prostuti.core.database.entity.OfflineQuestionEntity
import app.prostuti.core.database.entity.PendingOperationEntity
import app.prostuti.core.datastore.UserPreferencesDataSource
import app.prostuti.core.model.AnswerRecord
import app.prostuti.core.model.PracticeConfig
import app.prostuti.core.model.PracticeMode
import app.prostuti.core.network.NetworkJson
import app.prostuti.core.network.api.PracticeApi
import app.prostuti.core.network.api.RevisionApi
import app.prostuti.core.network.dto.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineSyncTest {
    private lateinit var context: Context
    private lateinit var db: ProstutiDatabase
    private lateinit var server: MockWebServer

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        db = Room.inMemoryDatabaseBuilder(context, ProstutiDatabase::class.java).allowMainThreadQueries().build()
        server = MockWebServer().apply { start() }
    }

    @After fun tearDown() { db.close(); server.shutdown() }

    private fun retrofit() = Retrofit.Builder().baseUrl(server.url("/")).client(OkHttpClient())
        .addConverterFactory(NetworkJson.asConverterFactory("application/json".toMediaType())).build()

    private fun op(id: String, at: Long, attempts: Int = 0) = PendingOperationEntity(id, SyncOps.NOTE_UPSERT, "{}", at, attempts)

    private fun worker(): SyncWorker {
        val api = retrofit().create(RevisionApi::class.java)
        val prefs = UserPreferencesDataSource(context)
        val dao = db.pendingOperationDao()
        return TestListenableWorkerBuilder<SyncWorker>(context).setWorkerFactory(object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
                SyncWorker(appContext, workerParameters, dao, api, prefs)
        }).build()
    }

    @Test fun `sync replays queued changes in order and clears applied ones`() = runBlocking {
        val dao = db.pendingOperationDao()
        dao.enqueue(op("b", 2000)); dao.enqueue(op("a", 1000)); dao.enqueue(op("c", 3000))
        server.enqueue(MockResponse().setBody("""{"data":{"appliedIds":["a","b"],"rejectedIds":["c"],"serverTime":"2026-11-25T10:00:00Z"}}"""))
        server.enqueue(MockResponse().setBody("""{"data":{"appliedIds":[],"rejectedIds":["c"],"serverTime":"2026-11-25T10:00:01Z"}}"""))

        val result = worker().doWork()

        assertIs<ListenableWorker.Result.Success>(result)
        val body = NetworkJson.decodeFromString(SyncBatchDto.serializer(), server.takeRequest().body.readUtf8())
        assertEquals(listOf("a", "b", "c"), body.operations.map { it.id })
        val left = dao.oldest(10)
        assertEquals(listOf("c"), left.map { it.id })
        assertTrue(left.single().attempts >= 1)
        assertTrue(left.single().lastError!!.startsWith("rejected"))
    }

    @Test fun `operations rejected too many times are dropped so the queue never blocks`() = runBlocking {
        val dao = db.pendingOperationDao()
        dao.enqueue(op("bad", 1000, attempts = SyncWorker.MAX_ATTEMPTS - 1))
        server.enqueue(MockResponse().setBody("""{"data":{"appliedIds":[],"rejectedIds":["bad"],"serverTime":"2026-11-25T10:00:00Z"}}"""))
        worker().doWork()
        assertTrue(dao.oldest(10).isEmpty())
    }

    @Test fun `network failure keeps changes queued and asks WorkManager to retry`() = runBlocking {
        db.pendingOperationDao().enqueue(op("a", 1000))
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertIs<ListenableWorker.Result.Retry>(worker().doWork())
        assertEquals(1, db.pendingOperationDao().oldest(10).size)
    }

    @Test fun `practice works offline from downloaded questions and queues the result`() = runBlocking {
        val question = QuestionDto(
            id = "q1", type = "MCQ", stem = LocalizedDto("2 + 2 = ?"),
            options = listOf(OptionDto("a", LocalizedDto("3")), OptionDto("b", LocalizedDto("4"))),
            difficulty = "EASY", provenance = "OFFICIAL", topicId = "t1",
            topicName = LocalizedDto("Arithmetic"), subjectName = LocalizedDto("Math"),
            solution = SolutionDto(listOf("b"), LocalizedDto("Because 2 + 2 = 4.")),
        )
        db.offlineQuestionDao().upsertAll(listOf(OfflineQuestionEntity("q1", "t1", "t1", NetworkJson.encodeToString(QuestionDto.serializer(), question))))
        server.shutdown() // no network at all

        val outbox = Outbox(db.pendingOperationDao(), SyncScheduler(context))
        val repo = LivePracticeRepository(retrofit().create(PracticeApi::class.java), db.offlineQuestionDao(), db.offlineContentDao(), db.wrongAnswerDao(), outbox)

        val session = (repo.start(PracticeConfig(PracticeMode.TOPIC, topicIds = listOf("t1"))) as AppResult.Success).data
        assertTrue(session.isOffline)
        val result = (repo.submit(session, listOf(AnswerRecord("q1", setOf("a"), Duration.ofSeconds(20)))) as AppResult.Success).data

        assertEquals(1, result.incorrect)
        assertEquals(false, result.isServerVerified)
        assertEquals(SyncOps.PRACTICE_RESULT, db.pendingOperationDao().oldest(10).single().type)
        assertEquals(1, db.wrongAnswerDao().observeAll().first().single().timesWrong)
    }
}
