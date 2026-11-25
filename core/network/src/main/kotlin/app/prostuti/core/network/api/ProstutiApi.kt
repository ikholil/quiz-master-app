package app.prostuti.core.network.api

import app.prostuti.core.network.dto.*
import retrofit2.http.*

/*
 * Typed client for the NestJS backend (OpenAPI: docs/api-contract.md).
 * Retrofit suspend functions are cancelled together with the calling coroutine, so leaving a
 * screen (viewModelScope) cancels its in-flight requests automatically.
 */

interface AuthApi {
    @POST("v1/auth/otp/request") suspend fun requestOtp(@Body body: OtpRequestDto): Envelope<OtpChallengeDto>
    @POST("v1/auth/otp/verify") suspend fun verifyOtp(@Body body: OtpVerifyDto): Envelope<AuthResponseDto>
    @POST("v1/auth/email/login") suspend fun emailLogin(@Body body: EmailLoginDto): Envelope<AuthResponseDto>
    @POST("v1/auth/email/register") suspend fun emailRegister(@Body body: EmailRegisterDto): Envelope<AuthResponseDto>
    @POST("v1/auth/password/forgot") suspend fun forgotPassword(@Body body: ForgotPasswordDto): Envelope<Unit?>
    @POST("v1/auth/logout") suspend fun logout(@Body body: RefreshRequestDto): Envelope<Unit?>
}

/** Refresh lives on its own interface: it is called from a client without the auth interceptor. */
interface RefreshApi {
    @POST("v1/auth/refresh") suspend fun refresh(@Body body: RefreshRequestDto): Envelope<TokensDto>
}

interface ProfileApi {
    @GET("v1/me/profile") suspend fun profile(): Envelope<ProfileDto>
    @PUT("v1/me/preferences") suspend fun savePreferences(@Body body: PreferencesDto): Envelope<ProfileDto>
    @POST("v1/planner/preview") suspend fun previewPlan(@Body body: PreferencesDto): Envelope<List<PlanTaskDto>>
    @POST("v1/me/onboarding/complete") suspend fun completeOnboarding(): Envelope<Unit?>
    @GET("v1/me/dashboard") suspend fun dashboard(): Envelope<DashboardDto>
    @GET("v1/me/analytics") suspend fun analytics(@Query("range") range: String): Envelope<AnalyticsDto>
}

interface ExamApi {
    @GET("v1/exams")
    suspend fun exams(
        @Query("q") query: String?,
        @Query("category") category: String?,
        @Query("bookmarked") bookmarkedOnly: Boolean?,
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int = 20,
    ): Envelope<List<ExamSummaryDto>>

    @GET("v1/exams/{id}") suspend fun exam(@Path("id") id: String): Envelope<ExamDetailDto>
    @PUT("v1/exams/{id}/bookmark") suspend fun bookmark(@Path("id") id: String): Envelope<Unit?>
    @DELETE("v1/exams/{id}/bookmark") suspend fun unbookmark(@Path("id") id: String): Envelope<Unit?>
    @GET("v1/exams/compare") suspend fun compare(@Query("ids") ids: String): Envelope<ComparisonDto>
    @PUT("v1/me/targets") suspend fun setTargets(@Body body: TargetsDto): Envelope<Unit?>
    @GET("v1/announcements") suspend fun announcements(): Envelope<List<AnnouncementDto>>
}

interface LearningApi {
    @GET("v1/exams/{id}/subjects") suspend fun subjects(@Path("id") examId: String): Envelope<List<SubjectDto>>
    @GET("v1/subjects/{id}/chapters") suspend fun chapters(@Path("id") subjectId: String): Envelope<List<ChapterDto>>
    @GET("v1/topics/{id}") suspend fun topic(@Path("id") topicId: String): Envelope<TopicDetailDto>
    @GET("v1/topics/{id}/offline-bundle") suspend fun topicBundle(@Path("id") topicId: String): Envelope<TopicBundleDto>
    @GET("v1/materials/{id}") suspend fun material(@Path("id") materialId: String): Envelope<MaterialDto>
    @PUT("v1/topics/{id}/progress") suspend fun setProgress(@Path("id") topicId: String, @Body body: TopicProgressDto): Envelope<Unit?>
    @PUT("v1/topics/{id}/bookmark") suspend fun bookmark(@Path("id") topicId: String): Envelope<Unit?>
    @DELETE("v1/topics/{id}/bookmark") suspend fun unbookmark(@Path("id") topicId: String): Envelope<Unit?>
    @PUT("v1/topics/{id}/note") suspend fun saveNote(@Path("id") topicId: String, @Body body: NoteBodyDto): Envelope<Unit?>
}

interface QuestionApi {
    @GET("v1/questions")
    suspend fun questions(
        @Query("q") query: String?,
        @Query("examId") examId: String?,
        @Query("subjectId") subjectId: String?,
        @Query("topicId") topicId: String?,
        @Query("difficulty") difficulty: String?,
        @Query("type") type: String?,
        @Query("provenance") provenance: String?,
        @Query("previousYear") previousYearOnly: Boolean?,
        @Query("bookmarked") bookmarkedOnly: Boolean?,
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int = 20,
    ): Envelope<List<QuestionDto>>

    @GET("v1/questions/{id}") suspend fun question(@Path("id") id: String): Envelope<QuestionDto>
    @GET("v1/questions/{id}/similar") suspend fun similar(@Path("id") id: String): Envelope<List<QuestionDto>>
    @PUT("v1/questions/{id}/bookmark") suspend fun bookmark(@Path("id") id: String): Envelope<Unit?>
    @DELETE("v1/questions/{id}/bookmark") suspend fun unbookmark(@Path("id") id: String): Envelope<Unit?>
    @PUT("v1/questions/{id}/note") suspend fun saveNote(@Path("id") id: String, @Body body: NoteBodyDto): Envelope<Unit?>
    @POST("v1/questions/{id}/reports") suspend fun report(@Path("id") id: String, @Body body: ReportDto): Envelope<Unit?>
    @GET("v1/me/wrong-answers") suspend fun wrongAnswers(): Envelope<List<WrongAnswerDto>>
    @POST("v1/me/wrong-answers/{id}/resolve") suspend fun resolveWrongAnswer(@Path("id") questionId: String): Envelope<Unit?>
    @GET("v1/me/collections") suspend fun collections(): Envelope<List<CollectionDto>>
    @POST("v1/me/collections") suspend fun createCollection(@Body body: CreateCollectionDto): Envelope<CollectionDto>
    @POST("v1/me/collections/{id}/questions") suspend fun addToCollection(@Path("id") id: String, @Body body: AddToCollectionDto): Envelope<Unit?>
}

interface PracticeApi {
    @POST("v1/practice/sessions") suspend fun start(@Body body: PracticeConfigDto): Envelope<PracticeSessionDto>
    @POST("v1/practice/sessions/{id}/submit") suspend fun submit(@Path("id") id: String, @Body body: SubmitPracticeDto): Envelope<PracticeResultDto>
    @GET("v1/practice/recommended-sets") suspend fun recommendedSets(): Envelope<List<PracticeSetDto>>
    @GET("v1/practice/sets/{id}/bundle") suspend fun setBundle(@Path("id") id: String): Envelope<QuestionSetBundleDto>
    @GET("v1/practice/daily-challenge") suspend fun dailyChallenge(): Envelope<PracticeConfigDto>
}

interface MockExamApi {
    @GET("v1/mocks") suspend fun mocks(@Query("examId") examId: String?): Envelope<List<MockSummaryDto>>
    @GET("v1/mocks/{id}") suspend fun mock(@Path("id") id: String): Envelope<MockSummaryDto>
    @POST("v1/mocks/{id}/attempts") suspend fun startAttempt(@Path("id") id: String): Envelope<AttemptDto>
    @GET("v1/attempts/{id}") suspend fun attempt(@Path("id") id: String): Envelope<AttemptDto>
    @PUT("v1/attempts/{id}/answers") suspend fun saveAnswers(@Path("id") id: String, @Body body: SaveAnswersDto): Envelope<ServerTimeDto>
    @POST("v1/attempts/{id}/submit") suspend fun submit(@Path("id") id: String, @Body body: SubmitAttemptDto): Envelope<MockResultDto>
    @GET("v1/attempts/{id}/result") suspend fun result(@Path("id") id: String): Envelope<MockResultDto>
    @GET("v1/me/attempts") suspend fun history(): Envelope<List<AttemptHistoryDto>>
}

interface PlannerApi {
    @GET("v1/planner/plan") suspend fun plan(@Query("examId") examId: String?): Envelope<StudyPlanDto>
    @PATCH("v1/planner/tasks/{id}") suspend fun setStatus(@Path("id") id: String, @Body body: TaskStatusDto): Envelope<Unit?>
    @POST("v1/planner/recover") suspend fun recover(): Envelope<StudyPlanDto>
    @POST("v1/planner/regenerate") suspend fun regenerate(@Body body: RegeneratePlanDto): Envelope<StudyPlanDto>
}

interface RevisionApi {
    @GET("v1/flashcards/decks") suspend fun decks(): Envelope<List<DeckDto>>
    @GET("v1/flashcards") suspend fun flashcards(@Query("deckId") deckId: String?, @Query("updatedSince") updatedSince: String?): Envelope<List<FlashcardDto>>
    @POST("v1/flashcards") suspend fun create(@Body body: CreateFlashcardDto): Envelope<FlashcardDto>
    @POST("v1/flashcards/batch") suspend fun saveBatch(@Body body: FlashcardBatchDto): Envelope<Unit?>
    @GET("v1/revision/quick") suspend fun quickRevision(@Query("examId") examId: String?, @Query("kind") kind: String?): Envelope<List<FlashcardDto>>
    @GET("v1/notes") suspend fun notes(@Query("updatedSince") updatedSince: String?): Envelope<List<NoteDto>>
    @POST("v1/sync") suspend fun sync(@Body body: SyncBatchDto): Envelope<SyncResultDto>
}

interface TutorApi {
    @GET("v1/ai/usage") suspend fun usage(): Envelope<AiUsageDto>
    @POST("v1/ai/tutor/messages") suspend fun ask(@Body body: TutorRequestDto): Envelope<TutorResponseDto>
}

interface CommunityApi {
    @GET("v1/groups") suspend fun groups(): Envelope<List<GroupDto>>
    @POST("v1/groups/{id}/members") suspend fun join(@Path("id") id: String): Envelope<Unit?>
    @DELETE("v1/groups/{id}/members/me") suspend fun leave(@Path("id") id: String): Envelope<Unit?>
    @GET("v1/groups/{id}/leaderboard") suspend fun leaderboard(@Path("id") id: String): Envelope<LeaderboardDto>
    @GET("v1/discussions") suspend fun discussions(@Query("exam") exam: String?, @Query("cursor") cursor: String?): Envelope<List<DiscussionDto>>
    @GET("v1/discussions/{id}/replies") suspend fun replies(@Path("id") id: String): Envelope<List<ReplyDto>>
    @POST("v1/discussions") suspend fun post(@Body body: PostDiscussionDto): Envelope<DiscussionDto>
    @POST("v1/discussions/{id}/replies") suspend fun reply(@Path("id") id: String, @Body body: PostReplyDto): Envelope<ReplyDto>
    @POST("v1/content/{id}/reports") suspend fun report(@Path("id") id: String, @Body body: ContentReportDto): Envelope<Unit?>
}

interface AccountApi {
    @GET("v1/me/subscription") suspend fun subscription(): Envelope<SubscriptionDto>
    @GET("v1/billing/plans") suspend fun plans(): Envelope<List<PlanDto>>
    @POST("v1/billing/checkout") suspend fun checkout(@Body body: CheckoutRequestDto): Envelope<CheckoutDto>
    @GET("v1/me/privacy") suspend fun privacy(): Envelope<PrivacyDto>
    @PUT("v1/me/privacy") suspend fun updatePrivacy(@Body body: PrivacyDto): Envelope<PrivacyDto>
    @POST("v1/me/data-export") suspend fun requestExport(): Envelope<ExportDto>
    @POST("v1/me/deletion-request") suspend fun requestDeletion(@Body body: DeletionRequestDto): Envelope<Unit?>
    @GET("v1/me/achievements") suspend fun achievements(): Envelope<List<AchievementDto>>
    @GET("v1/support/tickets") suspend fun tickets(): Envelope<List<TicketDto>>
    @POST("v1/support/tickets") suspend fun createTicket(@Body body: CreateTicketDto): Envelope<TicketDto>
    @GET("v1/me/notifications") suspend fun notifications(): Envelope<List<NotificationDto>>
    @POST("v1/me/devices") suspend fun registerDevice(@Body body: DeviceDto): Envelope<Unit?>
}
