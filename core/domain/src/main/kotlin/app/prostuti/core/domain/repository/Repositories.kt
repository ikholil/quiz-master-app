package app.prostuti.core.domain.repository

import app.prostuti.core.common.AppResult
import app.prostuti.core.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/*
 * Repository contracts. Live implementations live in :core:data and talk to the NestJS backend;
 * :core:mockbackend provides development-only implementations used by the `mock` build flavor.
 */

sealed interface SessionState {
    data object Unknown : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val userId: String, val onboardingCompleted: Boolean) : SessionState
}

interface AuthRepository {
    val session: Flow<SessionState>
    suspend fun requestOtp(phone: String): AppResult<OtpChallenge>
    suspend fun verifyOtp(challengeId: String, code: String): AppResult<AuthSession>
    suspend fun signInWithEmail(email: String, password: String): AppResult<AuthSession>
    suspend fun registerWithEmail(name: String, email: String, password: String): AppResult<AuthSession>
    suspend fun requestPasswordReset(email: String): AppResult<Unit>
    suspend fun signOut()
}

interface ProfileRepository {
    val profile: Flow<StudentProfile?>
    suspend fun refresh(): AppResult<StudentProfile>
    suspend fun savePreferences(preferences: PreparationPreferences): AppResult<StudentProfile>
    /** Ask the planner for an initial plan preview during onboarding. */
    suspend fun previewInitialPlan(preferences: PreparationPreferences): AppResult<List<PlanTask>>
    suspend fun completeOnboarding(): AppResult<Unit>
}

enum class ThemePreference { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val language: AppLanguage,
    val theme: ThemePreference,
    val dataSaver: Boolean,
    val downloadOnWifiOnly: Boolean,
    val studyReminderHour: Int?,
)

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setTheme(theme: ThemePreference)
    suspend fun setDataSaver(enabled: Boolean)
    suspend fun setDownloadOnWifiOnly(enabled: Boolean)
    suspend fun setStudyReminderHour(hour: Int?)
}

interface DashboardRepository {
    suspend fun dashboard(): AppResult<Dashboard>
}

interface ExamRepository {
    suspend fun exams(filter: ExamFilter, cursor: String? = null): AppResult<Page<ExamSummary>>
    suspend fun exam(examId: String): AppResult<ExamDetail>
    suspend fun setBookmarked(examId: String, bookmarked: Boolean): AppResult<Unit>
    suspend fun compare(examIds: List<String>): AppResult<SyllabusComparison>
    suspend fun setTargets(targets: List<TargetExam>): AppResult<Unit>
    suspend fun announcements(): AppResult<List<Announcement>>
}

interface LearningRepository {
    suspend fun subjects(examId: String): AppResult<List<Subject>>
    suspend fun chapters(subjectId: String): AppResult<List<Chapter>>
    suspend fun topic(topicId: String): AppResult<TopicDetail>
    suspend fun material(materialId: String): AppResult<LearningMaterial>
    /** Works offline: completion is queued and synced. */
    suspend fun setTopicCompleted(topicId: String, completed: Boolean): AppResult<Unit>
    suspend fun setTopicBookmarked(topicId: String, bookmarked: Boolean): AppResult<Unit>
    suspend fun saveTopicNote(topicId: String, note: String): AppResult<Unit>
}

interface QuestionRepository {
    suspend fun questions(filter: QuestionFilter, cursor: String? = null): AppResult<Page<Question>>
    /** Returns the question with its solution; used outside active exams only. */
    suspend fun question(questionId: String): AppResult<Question>
    suspend fun similar(questionId: String): AppResult<List<Question>>
    suspend fun setBookmarked(questionId: String, bookmarked: Boolean): AppResult<Unit>
    suspend fun saveNote(questionId: String, note: String): AppResult<Unit>
    suspend fun report(questionId: String, reason: QuestionReportReason, details: String): AppResult<Unit>
    fun wrongAnswers(): Flow<List<WrongAnswerEntry>>
    suspend fun markWrongAnswerResolved(questionId: String): AppResult<Unit>
    suspend fun collections(): AppResult<List<QuestionCollection>>
    suspend fun createCollection(name: String): AppResult<QuestionCollection>
    suspend fun addToCollection(collectionId: String, questionId: String): AppResult<Unit>
}

interface PracticeRepository {
    /** Starts online when possible; falls back to downloaded questions when offline. */
    suspend fun start(config: PracticeConfig): AppResult<PracticeSession>
    suspend fun submit(session: PracticeSession, answers: List<AnswerRecord>): AppResult<PracticeResult>
    suspend fun recommendedSets(): AppResult<List<PracticeSetSummary>>
    suspend fun dailyChallenge(): AppResult<PracticeConfig>
}

interface MockExamRepository {
    suspend fun mocks(examId: String?): AppResult<List<MockExamSummary>>
    suspend fun mock(mockId: String): AppResult<MockExamSummary>
    /** Starts or resumes the server-side attempt. */
    suspend fun startAttempt(mockId: String): AppResult<MockAttempt>
    suspend fun resumeAttempt(attemptId: String): AppResult<MockAttempt>
    /** Autosave. The server rejects writes after its deadline. */
    suspend fun saveAnswers(attemptId: String, answers: Map<String, Set<String>>, markedForReview: Set<String>): AppResult<Unit>
    suspend fun submit(attemptId: String, answers: Map<String, Set<String>>): AppResult<MockResult>
    suspend fun result(attemptId: String): AppResult<MockResult>
    suspend fun history(): AppResult<List<MockAttemptHistoryItem>>
}

interface AnalyticsRepository {
    suspend fun analytics(range: AnalyticsRange): AppResult<PerformanceAnalytics>
}

interface TutorRepository {
    suspend fun usage(): AppResult<AiUsage>
    suspend fun ask(
        conversationId: String?,
        intent: TutorIntent,
        message: String,
        context: TutorContext,
        language: AppLanguage,
    ): AppResult<Pair<String, ChatMessage>>
    suspend fun saveFlashcards(cards: List<Flashcard>): AppResult<Unit>
}

interface PlannerRepository {
    suspend fun plan(examId: String?): AppResult<StudyPlan>
    suspend fun setTaskStatus(taskId: String, status: PlanTaskStatus): AppResult<Unit>
    /** Asks the engine to redistribute missed tasks over the coming days. */
    suspend fun recoverMissedTasks(): AppResult<StudyPlan>
    suspend fun regenerate(dailyMinutes: Int, examDate: LocalDate?): AppResult<StudyPlan>
}

interface RevisionRepository {
    fun overview(): Flow<RevisionOverview>
    fun decks(): Flow<List<FlashcardDeck>>
    suspend fun dueCards(deckId: String?, limit: Int): List<Flashcard>
    suspend fun review(card: Flashcard, grade: RecallGrade): Flashcard
    suspend fun createCard(deckId: String, front: String, back: String): AppResult<Flashcard>
    fun notes(): Flow<List<Note>>
    suspend fun saveNote(note: Note): AppResult<Note>
    suspend fun deleteNote(noteId: String): AppResult<Unit>
    suspend fun quickRevision(examId: String?, kind: DeckKind?): AppResult<List<Flashcard>>
    suspend fun refresh(): AppResult<Unit>
}

interface CommunityRepository {
    suspend fun groups(): AppResult<List<StudyGroup>>
    suspend fun joinGroup(groupId: String): AppResult<Unit>
    suspend fun leaveGroup(groupId: String): AppResult<Unit>
    suspend fun discussions(examShortName: String?, cursor: String? = null): AppResult<Page<Discussion>>
    suspend fun replies(discussionId: String): AppResult<List<DiscussionReply>>
    suspend fun postDiscussion(title: String, body: String, questionId: String?): AppResult<Discussion>
    suspend fun reply(discussionId: String, body: String): AppResult<DiscussionReply>
    suspend fun report(contentId: String, reason: String): AppResult<Unit>
    suspend fun leaderboard(groupId: String): AppResult<Leaderboard>
}

interface AccountRepository {
    val subscription: Flow<SubscriptionStatus>
    suspend fun refreshSubscription(): AppResult<SubscriptionStatus>
    suspend fun plans(): AppResult<List<SubscriptionPlan>>
    /** Returns a backend-issued checkout URL (bKash/Nagad/card handled by the payment gateway). */
    suspend fun checkout(planId: String): AppResult<String>
    suspend fun privacy(): AppResult<PrivacySettings>
    suspend fun updatePrivacy(settings: PrivacySettings): AppResult<PrivacySettings>
    suspend fun requestDataExport(): AppResult<DataExportRequest>
    suspend fun requestAccountDeletion(reason: String): AppResult<Unit>
    suspend fun achievements(): AppResult<List<Achievement>>
    suspend fun tickets(): AppResult<List<SupportTicket>>
    suspend fun createTicket(subject: String, message: String): AppResult<SupportTicket>
    suspend fun notifications(): AppResult<List<AppNotification>>
    suspend fun registerPushToken(token: String): AppResult<Unit>
}

interface OfflineRepository {
    fun downloads(): Flow<List<DownloadItem>>
    fun storage(): Flow<StorageUsage>
    fun syncStatus(): Flow<SyncStatus>
    suspend fun downloadTopic(topicId: String): AppResult<Unit>
    suspend fun downloadQuestionSet(setId: String): AppResult<Unit>
    suspend fun downloadDeck(deckId: String): AppResult<Unit>
    suspend fun remove(downloadId: String)
    suspend fun clearCache()
    fun requestSync()
}

interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
