package app.prostuti.core.mockbackend

import app.prostuti.core.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** Shared mutable state behind all mock repositories, so actions in one screen affect the others. */
@Singleton
class MockStore @Inject constructor() {
    val signedInUser = MutableStateFlow<String?>(null)
    val onboarded = MutableStateFlow(false)
    val profile = MutableStateFlow<StudentProfile?>(null)

    val bookmarkedExams = mutableSetOf("bcs")
    val completedTopics = ConcurrentHashMap.newKeySet<String>().apply { addAll(listOf("t-1971", "t-nazrul", "t-synonyms")) }
    val bookmarkedTopics = ConcurrentHashMap.newKeySet<String>()
    val bookmarkedQuestions = ConcurrentHashMap.newKeySet<String>()
    val topicNotes = ConcurrentHashMap<String, String>()
    val questionNotes = ConcurrentHashMap<String, String>()
    val topicAccuracy = ConcurrentHashMap<String, Pair<Int, Int>>().apply {
        put("t-1971", 8 to 10); put("t-nazrul", 4 to 5); put("t-synonyms", 7 to 8); put("t-interest", 2 to 6)
        put("t-identity", 1 to 4); put("t-integrity", 0 to 2); put("t-saarc", 1 to 3)
    }
    val wrongAnswers = MutableStateFlow<List<WrongAnswerEntry>>(emptyList())
    val collections = mutableListOf(QuestionCollection("c1", "Before exam", 0, Instant.now()))
    val collectionItems = ConcurrentHashMap<String, MutableSet<String>>()

    val attempts = ConcurrentHashMap<String, MockAttempt>()
    val results = ConcurrentHashMap<String, MockResult>()
    val planStatus = ConcurrentHashMap<String, PlanTaskStatus>()
    var studiedTodayMinutes = 35

    val flashcards = MutableStateFlow(SampleRevision.cards())
    val notes = MutableStateFlow(SampleRevision.notes())
    val downloads = MutableStateFlow(
        listOf(DownloadItem("t-1971", DownloadKind.MATERIAL, t("Liberation War of 1971", "১৯৭১ সালের মুক্তিযুদ্ধ"), 412_000, DownloadState.COMPLETED, 1f, Instant.now().minusSeconds(86_400))),
    )
    val pendingChanges = MutableStateFlow(0)

    fun mastery(topicId: String): Mastery {
        val (c, total) = topicAccuracy[topicId] ?: return Mastery(0f)
        return Mastery(if (total == 0) 0f else (c.toFloat() / total).coerceIn(0f, 1f) * 0.95f)
    }

    fun record(questionId: String, correct: Boolean) {
        val q = SampleQuestions.byId(questionId) ?: return
        val (c, total) = topicAccuracy[q.topicId] ?: (0 to 0)
        topicAccuracy[q.topicId] = (c + if (correct) 1 else 0) to total + 1
    }

    fun newId() = UUID.randomUUID().toString().take(8)
    fun today(): LocalDate = LocalDate.now()
}

/** Simulated network latency so loading states are visible during development. */
internal suspend fun latency(ms: Long = 350) = delay(ms)
