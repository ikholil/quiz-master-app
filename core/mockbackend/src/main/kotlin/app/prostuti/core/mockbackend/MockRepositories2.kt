package app.prostuti.core.mockbackend

import app.prostuti.core.common.AppError
import app.prostuti.core.common.AppResult
import app.prostuti.core.common.asSuccess
import app.prostuti.core.domain.repository.*
import app.prostuti.core.domain.revision.SpacedRepetition
import app.prostuti.core.model.*
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockDashboardRepository @Inject constructor(private val store: MockStore) : DashboardRepository {
    override suspend fun dashboard(): AppResult<Dashboard> {
        latency(600)
        val profile = store.profile.value
        val days = SamplePlan.days(store, profile?.dailyStudyMinutes ?: 60, profile?.weakSubjectIds.orEmpty())
        val today = LocalDate.now()
        val weak = SampleContent.topics.filter { store.topicAccuracy[it.id] != null }
            .sortedBy { store.mastery(it.id).value }.take(3).map {
                val (c, total) = store.topicAccuracy.getValue(it.id)
                WeakTopic(it.id, it.name, SampleContent.subjects[it.subjectId] ?: t(""), c.toFloat() / total,
                    t("$c of $total correct in recent practice.", "সাম্প্রতিক অনুশীলনে $total-এর মধ্যে $c সঠিক।"))
            }
        val history = store.results.values.maxByOrNull { it.submittedAt }?.let {
            MockAttemptHistoryItem(it.attemptId, it.mock.title, it.submittedAt, it.score, it.maxScore, it.negativeMarks, it.percentile)
        } ?: SamplePlan.pastAttempts().first()
        return Dashboard(
            greetingName = profile?.fullName?.substringBefore(' ').orEmpty(),
            primaryTarget = profile?.primaryTarget ?: TargetExam("bcs", SampleContent.exams.first().summary.name, today.plusDays(96), true),
            continueLearning = ContinueLearning("t-interest", t("Simple & compound interest", "সরল ও চক্রবৃদ্ধি মুনাফা"), t("Mathematical reasoning", "গাণিতিক যুক্তি"), 0.4f),
            todayTasks = days.first { it.date == today }.tasks,
            dailyTargetMinutes = profile?.dailyStudyMinutes ?: 60,
            studiedTodayMinutes = store.studiedTodayMinutes,
            streak = Streak(6, 14, (0..5).map { today.minusDays(it.toLong()) }),
            syllabusCompletion = store.completedTopics.size.toFloat() / SampleContent.topics.size,
            overallMastery = Mastery(SampleContent.topics.map { store.mastery(it.id).value }.average().toFloat()),
            weakTopics = weak,
            revisionDueCount = store.flashcards.value.count { SpacedRepetition.isDue(it.srs, Instant.now()) } + store.wrongAnswers.value.count { !it.resolved },
            recentMock = history,
            recommendedSets = SamplePlan.sets(store),
            announcements = SamplePlan.announcements(),
            weekly = WeeklySummary(312, 184, 0.68f, 0.06f, listOf(45, 60, 30, 75, 50, 20, store.studiedTodayMinutes)),
        ).asSuccess()
    }
}

@Singleton
class MockAnalyticsRepository @Inject constructor(private val store: MockStore) : AnalyticsRepository {
    override suspend fun analytics(range: AnalyticsRange): AppResult<PerformanceAnalytics> {
        latency(500)
        val today = LocalDate.now()
        val points = when (range) { AnalyticsRange.WEEK -> 7; AnalyticsRange.MONTH -> 10; AnalyticsRange.ALL -> 12 }
        val subjects = SampleContent.bcsPrelimSubjects.map { s ->
            val topics = SampleContent.topics.filter { it.subjectId == s.subjectId }
            val acc = topics.mapNotNull { store.topicAccuracy[it.id] }.let { l -> if (l.isEmpty()) 0f else l.sumOf { it.first }.toFloat() / l.sumOf { it.second }.coerceAtLeast(1) }
            val attempted = topics.mapNotNull { store.topicAccuracy[it.id] }.sumOf { it.second }
            SubjectPerformance(s.subjectId, s.subjectName, acc, attempted, Mastery((acc * 0.9f).coerceIn(0f, 1f)), if (s.subjectId == "math") 74f else 28f + s.marks.toFloat() / 3)
        }
        return PerformanceAnalytics(
            overallAccuracy = 0.68f, questionsAttempted = 1240, avgSecondsPerQuestion = 41f, negativeMarksLost = 23.5,
            syllabusCompletion = store.completedTopics.size.toFloat() / SampleContent.topics.size,
            subjects = subjects,
            accuracyTrend = (0 until points).map { TrendPoint(today.minusDays((points - it).toLong() * 3), 0.52f + it * 0.015f + if (it % 3 == 0) -0.03f else 0f) },
            mockScoreTrend = listOf(92f, 98.5f, 104.5f, 111f).mapIndexed { i, v -> TrendPoint(today.minusDays((4 - i) * 7L), v) },
            speedVsAccuracy = subjects.map { it.subjectName to (it.avgSecondsPerQuestion to it.accuracy) },
            repeatedMistakes = listOf(
                RepeatedMistake("t-1971", t("Liberation War of 1971", "১৯৭১ সালের মুক্তিযুদ্ধ"), t("Mixing up formation (10 Apr) and oath (17 Apr) dates", "সরকার গঠন (১০ এপ্রিল) ও শপথ (১৭ এপ্রিল) তারিখ গুলিয়ে ফেলা"), 4),
                RepeatedMistake("t-identities", t("Algebraic identities", "বীজগাণিতিক সূত্র"), t("Forgetting the −2 term when squaring x + 1/x", "x + 1/x বর্গ করার সময় −২ বাদ পড়া"), 3),
            ),
            revisionRetention = 0.81f, studyDaysLast30 = 22,
            consistency = (0 until 28).map { if (it % 6 == 5) 0 else 20 + (it * 13) % 60 },
            recommendations = listOf(
                Recommendation("r1", t("Do a math speed drill", "গণিত স্পিড ড্রিল করুন"),
                    t("Math takes you 74s per question, double your average.", "গণিতে প্রতি প্রশ্নে ৭৪ সেকেন্ড লাগছে — আপনার গড়ের দ্বিগুণ।"), RecommendedAction.OpenPracticeSet("set-math")),
                Recommendation("r2", t("Review the 1971 date trap", "১৯৭১-এর তারিখের ফাঁদটি দেখে নিন"),
                    t("Same mistake 4 times this month.", "এ মাসে একই ভুল ৪ বার।"), RecommendedAction.StudyTopic("t-1971")),
            ),
        ).asSuccess()
    }
}

@Singleton
class MockTutorRepository @Inject constructor(private val store: MockStore) : TutorRepository {
    private var used = 3
    override suspend fun usage() = AiUsage(used, 30).asSuccess()

    /** Canned, clearly-labelled development responses. The live backend uses RAG over approved content. */
    override suspend fun ask(conversationId: String?, intent: TutorIntent, message: String, context: TutorContext, language: AppLanguage): AppResult<Pair<String, ChatMessage>> {
        latency(1200)
        if (used >= 30) return AppResult.Failure(AppError.RateLimited(null))
        used++
        val q = context.questionId?.let { SampleQuestions.byId(it) }
        val bn = language == AppLanguage.BENGALI
        val explanation = q?.solution?.explanation?.resolve(language)
        val text = when (intent) {
            TutorIntent.HINT -> if (bn) "ইঙ্গিত: প্রশ্নের মূল শব্দটি খেয়াল করুন এবং যে অপশনগুলো নিশ্চিতভাবে ভুল সেগুলো আগে বাদ দিন।" else "Hint: focus on the key term in the question and eliminate options you're sure are wrong first."
            TutorIntent.WHY_OTHERS_WRONG -> q?.let { qq ->
                qq.options.filter { it.id !in qq.solution!!.correctOptionIds }.joinToString("\n") { o ->
                    "• ${o.text.resolve(language)}: " + (qq.solution!!.optionExplanations[o.id]?.resolve(language) ?: if (bn) "প্রশ্নের শর্তের সাথে মেলে না।" else "does not satisfy the question.")
                }
            } ?: (if (bn) "কোন প্রশ্নটি নিয়ে জানতে চান, সেটি খুলে আবার জিজ্ঞেস করুন।" else "Open a question and ask again so I know which options to compare.")
            TutorIntent.FLASHCARDS -> if (bn) "এই টপিক থেকে ৩টি ফ্ল্যাশকার্ড তৈরি করেছি। নিচে দেখে সংরক্ষণ করুন।" else "I made 3 flashcards from this topic. Review and save them below."
            TutorIntent.SOLVE_STEP_BY_STEP -> explanation?.let { (if (bn) "ধাপে ধাপে:\n" else "Step by step:\n") + it.split(";", ". ").filter { s -> s.isNotBlank() }.mapIndexed { i, s -> "${i + 1}. ${s.trim()}" }.joinToString("\n") }
                ?: (if (bn) "অঙ্কটি লিখে পাঠান, ধাপে ধাপে সমাধান দেখাব।" else "Send me the problem and I'll solve it step by step.")
            else -> explanation ?: if (bn) "ভালো প্রশ্ন! অনুমোদিত স্টাডি ম্যাটেরিয়াল থেকে উত্তর খুঁজে দিচ্ছি: মূল ধারণাটি হলো বিষয়টিকে ছোট অংশে ভাগ করে প্রতিটি অংশের সংজ্ঞা মনে রাখা।" else "Good question! From the approved study material: break the idea into smaller parts and remember the definition of each part."
        }
        val cards = if (intent == TutorIntent.FLASHCARDS) SampleRevision.cards().take(3).map { it.copy(id = "ai-${store.newId()}", origin = FlashcardOrigin.AI_APPROVED) } else emptyList()
        val reply = ChatMessage(
            id = store.newId(), role = ChatRole.ASSISTANT, text = text,
            sources = if (q != null || context.topicId != null) listOf(SourceReference("Prostuti study notes", "Prostuti editorial", null, Instant.now().minusSeconds(86_400 * 20))) else emptyList(),
            createdAt = Instant.now(),
            suggestedFollowUps = listOf(TutorIntent.WHY_OTHERS_WRONG, TutorIntent.FLASHCARDS, TutorIntent.PRACTICE_QUESTIONS).filter { it != intent },
            generatedFlashcards = cards,
        )
        return ((conversationId ?: "conv-${store.newId()}") to reply).asSuccess()
    }

    override suspend fun saveFlashcards(cards: List<Flashcard>): AppResult<Unit> {
        store.flashcards.update { it + cards.map { c -> c.copy(deckId = "deck-facts") } }
        return Unit.asSuccess()
    }
}

@Singleton
class MockPlannerRepository @Inject constructor(private val store: MockStore) : PlannerRepository {
    private fun build(): StudyPlan {
        val profile = store.profile.value
        val days = SamplePlan.days(store, profile?.dailyStudyMinutes ?: 60, profile?.weakSubjectIds.orEmpty())
        val target = profile?.primaryTarget
        return StudyPlan(
            "plan-1", target?.examId ?: "bcs", target?.examName ?: SampleContent.exams.first().summary.name,
            target?.examDate ?: LocalDate.now().plusDays(96), days,
            days.sumOf { d -> d.tasks.count { it.status == PlanTaskStatus.MISSED } },
            listOf(PlanAdjustment("adj1", t("Added 2 extra math sessions after your last model test.", "শেষ মডেল টেস্টের পর গণিতে ২টি অতিরিক্ত সেশন যোগ করা হয়েছে।"), LocalDate.now().minusDays(1))),
        )
    }

    override suspend fun plan(examId: String?): AppResult<StudyPlan> { latency(); return build().asSuccess() }
    override suspend fun setTaskStatus(taskId: String, status: PlanTaskStatus): AppResult<Unit> { store.planStatus[taskId] = status; return Unit.asSuccess() }

    override suspend fun recoverMissedTasks(): AppResult<StudyPlan> {
        latency(700)
        build().days.flatMap { it.tasks }.filter { it.status == PlanTaskStatus.MISSED }.forEach { store.planStatus[it.id] = PlanTaskStatus.RESCHEDULED }
        return build().asSuccess()
    }

    override suspend fun regenerate(dailyMinutes: Int, examDate: LocalDate?): AppResult<StudyPlan> {
        latency(800)
        store.profile.value = store.profile.value?.copy(dailyStudyMinutes = dailyMinutes)
        return build().asSuccess()
    }
}

@Singleton
class MockRevisionRepository @Inject constructor(private val store: MockStore) : RevisionRepository {
    override fun decks(): Flow<List<FlashcardDeck>> = store.flashcards.map { cards ->
        val now = Instant.now()
        SampleRevision.decks.map { (id, name, kind) ->
            val inDeck = cards.filter { it.deckId == id }
            FlashcardDeck(id, name, kind, inDeck.size, inDeck.count { SpacedRepetition.isDue(it.srs, now) }, id == "deck-facts")
        }
    }

    override fun overview(): Flow<RevisionOverview> = combine(decks(), store.wrongAnswers, store.notes) { decks, wrong, notes ->
        RevisionOverview(decks.sumOf { it.dueCount }, wrong.count { !it.resolved }, store.bookmarkedQuestions.size, notes.size, decks)
    }

    override suspend fun dueCards(deckId: String?, limit: Int): List<Flashcard> {
        val now = Instant.now()
        return store.flashcards.value.filter { (deckId == null || it.deckId == deckId) && SpacedRepetition.isDue(it.srs, now) }.take(limit)
    }

    override suspend fun review(card: Flashcard, grade: RecallGrade): Flashcard {
        val updated = card.copy(srs = SpacedRepetition.review(card.srs, grade, Instant.now()))
        store.flashcards.update { list -> list.map { if (it.id == card.id) updated else it } }
        store.pendingChanges.update { it + 1 }
        return updated
    }

    override suspend fun createCard(deckId: String, front: String, back: String): AppResult<Flashcard> {
        val card = Flashcard(store.newId(), deckId, t(front, front), t(back, back), FlashcardOrigin.USER, null, SrsState.new(Instant.now()))
        store.flashcards.update { it + card }
        return card.asSuccess()
    }

    override fun notes(): Flow<List<Note>> = store.notes.map { it.sortedByDescending { n -> n.updatedAt } }

    override suspend fun saveNote(note: Note): AppResult<Note> {
        val saved = note.copy(id = note.id.ifBlank { store.newId() }, updatedAt = Instant.now(), isSynced = true)
        store.notes.update { list -> listOf(saved) + list.filter { it.id != saved.id } }
        return saved.asSuccess()
    }

    override suspend fun deleteNote(noteId: String): AppResult<Unit> {
        store.notes.update { list -> list.filter { it.id != noteId } }
        return Unit.asSuccess()
    }

    override suspend fun quickRevision(examId: String?, kind: DeckKind?): AppResult<List<Flashcard>> {
        latency()
        val deckIds = SampleRevision.decks.filter { kind == null || it.third == kind }.map { it.first }.toSet()
        return store.flashcards.value.filter { it.deckId in deckIds }.asSuccess()
    }

    override suspend fun refresh(): AppResult<Unit> = Unit.asSuccess()
}

@Singleton
class MockCommunityRepository @Inject constructor(private val store: MockStore) : CommunityRepository {
    private val joined = mutableSetOf("g1")
    private val discussions = MutableStateFlow(listOf(
        Discussion("d1", "Mujibnagar: formed vs oath date?", "Many guides mix these up. Is the oath on 17 April final?", "Tanvir", "q-mujibnagar", "BCS", 5, 23, Instant.now().minusSeconds(7200), true),
        Discussion("d2", "x + 1/x shortcut", "Is there a faster way than expanding every time?", "Nusrat", "q-identity", "BCS", 3, 11, Instant.now().minusSeconds(86_400), false),
        Discussion("d3", "NTRCA prelim: how many hours per day?", "Working full-time. How do you plan?", "Rafi", null, "NTRCA", 12, 40, Instant.now().minusSeconds(86_400 * 3), false),
    ))
    private val replies = mutableMapOf(
        "d1" to mutableListOf(
            DiscussionReply("r1", "Prostuti editor", "Yes. Formed 10 April 1971, oath 17 April 1971 at Baidyanathtala.", 18, true, true, Instant.now().minusSeconds(6000)),
            DiscussionReply("r2", "Mim", "I remember it as 'form first, swear later'.", 4, false, false, Instant.now().minusSeconds(5000)),
        ),
    )

    override suspend fun groups(): AppResult<List<StudyGroup>> {
        latency()
        return listOf(
            StudyGroup("g1", "BCS Dhaka morning batch", "BCS", 128, "g1" in joined, false, "Daily 6am practice and weekly mock review."),
            StudyGroup("g2", "Math for non-science students", null, 342, "g2" in joined, false, "Step-by-step help for arithmetic and algebra."),
            StudyGroup("g3", "NTRCA 2027 prep circle", "NTRCA", 76, "g3" in joined, true, "Invite-only accountability group."),
        ).asSuccess()
    }

    override suspend fun joinGroup(groupId: String): AppResult<Unit> { joined += groupId; return Unit.asSuccess() }
    override suspend fun leaveGroup(groupId: String): AppResult<Unit> { joined -= groupId; return Unit.asSuccess() }

    override suspend fun discussions(examShortName: String?, cursor: String?): AppResult<Page<Discussion>> {
        latency()
        return Page(discussions.value.filter { examShortName == null || it.examShortName == examShortName }, null).asSuccess()
    }

    override suspend fun replies(discussionId: String): AppResult<List<DiscussionReply>> { latency(); return replies[discussionId].orEmpty().asSuccess() }

    override suspend fun postDiscussion(title: String, body: String, questionId: String?): AppResult<Discussion> {
        latency()
        val name = store.profile.value?.fullName ?: "You"
        val d = Discussion(store.newId(), title, body, name, questionId, "BCS", 0, 0, Instant.now(), false)
        discussions.update { listOf(d) + it }
        return d.asSuccess()
    }

    override suspend fun reply(discussionId: String, body: String): AppResult<DiscussionReply> {
        val r = DiscussionReply(store.newId(), store.profile.value?.fullName ?: "You", body, 0, false, false, Instant.now())
        replies.getOrPut(discussionId) { mutableListOf() } += r
        discussions.update { list -> list.map { if (it.id == discussionId) it.copy(replyCount = it.replyCount + 1) else it } }
        return r.asSuccess()
    }

    override suspend fun report(contentId: String, reason: String): AppResult<Unit> { latency(); return Unit.asSuccess() }

    override suspend fun leaderboard(groupId: String): AppResult<Leaderboard> {
        latency()
        val me = store.profile.value?.fullName ?: "You"
        return Leaderboard(groupId, "This week", listOf(
            LeaderboardEntry(1, "Sadia", 1840, false), LeaderboardEntry(2, "Arif", 1715, false),
            LeaderboardEntry(3, me, 1630, true), LeaderboardEntry(4, "Mahin", 1502, false), LeaderboardEntry(5, "Jui", 1390, false),
        )).asSuccess()
    }
}

@Singleton
class MockAccountRepository @Inject constructor() : AccountRepository {
    private val status = MutableStateFlow(SubscriptionStatus.Free)
    override val subscription: Flow<SubscriptionStatus> = status
    private var privacy = PrivacySettings(showOnLeaderboards = true, shareProgressWithGroups = true, personalizedRecommendations = true, marketingNotifications = false)
    private val tickets = mutableListOf(SupportTicket("tk1", "Mock exam result not showing", TicketStatus.RESOLVED, Instant.now().minusSeconds(86_400 * 8), "Fixed — please update the app."))

    override suspend fun refreshSubscription() = status.value.asSuccess()
    override suspend fun plans(): AppResult<List<SubscriptionPlan>> {
        latency()
        val all = Entitlement.entries.toSet()
        return listOf(
            SubscriptionPlan("plus-1m", t("Plus · Monthly", "প্লাস · মাসিক"), 199, 1, all, false),
            SubscriptionPlan("plus-6m", t("Plus · 6 months", "প্লাস · ৬ মাস"), 899, 6, all, true),
            SubscriptionPlan("plus-12m", t("Plus · Yearly", "প্লাস · বার্ষিক"), 1499, 12, all, false),
        ).asSuccess()
    }
    override suspend fun checkout(planId: String): AppResult<String> = AppResult.Failure(AppError.Validation("Payments are disabled in development builds."))
    override suspend fun privacy() = privacy.asSuccess()
    override suspend fun updatePrivacy(settings: PrivacySettings): AppResult<PrivacySettings> { privacy = settings; return settings.asSuccess() }
    override suspend fun requestDataExport(): AppResult<DataExportRequest> { latency(); return DataExportRequest("ex1", Instant.now(), null, null).asSuccess() }
    override suspend fun requestAccountDeletion(reason: String): AppResult<Unit> { latency(); return Unit.asSuccess() }
    override suspend fun achievements(): AppResult<List<Achievement>> = listOf(
        Achievement("a1", t("First steps", "প্রথম পদক্ষেপ"), t("Finish your first practice session", "প্রথম অনুশীলন শেষ করুন"), Instant.now().minusSeconds(86_400 * 10), 1f),
        Achievement("a2", t("Week warrior", "সপ্তাহের যোদ্ধা"), t("Study 7 days in a row", "টানা ৭ দিন পড়ুন"), null, 6f / 7f),
        Achievement("a3", t("Sharpshooter", "নিখুঁত নিশানা"), t("Score 90% in a 20-question set", "২০ প্রশ্নের সেটে ৯০% পান"), null, 0.4f),
        Achievement("a4", t("Model citizen", "মডেল টেস্ট মাস্টার"), t("Complete 5 model tests", "৫টি মডেল টেস্ট দিন"), null, 0.4f),
    ).asSuccess()
    override suspend fun tickets() = tickets.toList().asSuccess()
    override suspend fun createTicket(subject: String, message: String): AppResult<SupportTicket> {
        latency()
        val tk = SupportTicket("tk${tickets.size + 1}", subject, TicketStatus.OPEN, Instant.now(), null)
        tickets.add(0, tk)
        return tk.asSuccess()
    }
    override suspend fun notifications(): AppResult<List<AppNotification>> = listOf(
        AppNotification("nt1", t("6-day streak!", "৬ দিনের ধারাবাহিকতা!"), t("Study today to reach 7.", "আজ পড়লে ৭ দিন পূর্ণ হবে।"), Instant.now().minusSeconds(3600), false),
        AppNotification("nt2", t("Your plan was adjusted", "আপনার পরিকল্পনা হালনাগাদ হয়েছে"), t("Two extra math sessions were added after your model test.", "মডেল টেস্টের পর গণিতে ২টি অতিরিক্ত সেশন যোগ হয়েছে।"), Instant.now().minusSeconds(86_400), true),
    ).asSuccess()
    override suspend fun registerPushToken(token: String) = Unit.asSuccess()
}

@Singleton
class MockOfflineRepository @Inject constructor(private val store: MockStore) : OfflineRepository {
    override fun downloads(): Flow<List<DownloadItem>> = store.downloads
    override fun storage(): Flow<StorageUsage> = store.downloads.map { list -> StorageUsage(list.filter { it.state == DownloadState.COMPLETED }.sumOf { it.sizeBytes }, 3_400_000, 5_800_000_000) }
    override fun syncStatus(): Flow<SyncStatus> = store.pendingChanges.map { SyncStatus(it, Instant.now().minusSeconds(900), false) }

    private suspend fun add(id: String, kind: DownloadKind, title: LocalizedText, size: Long): AppResult<Unit> {
        store.downloads.update { list -> list.filter { it.id != id } + DownloadItem(id, kind, title, size, DownloadState.DOWNLOADING, 0.3f, null) }
        latency(1200)
        store.downloads.update { list -> list.map { if (it.id == id) it.copy(state = DownloadState.COMPLETED, progress = 1f, downloadedAt = Instant.now()) else it } }
        return Unit.asSuccess()
    }

    override suspend fun downloadTopic(topicId: String): AppResult<Unit> {
        val topic = SampleContent.topics.firstOrNull { it.id == topicId } ?: return AppResult.Failure(AppError.NotFound)
        return add(topicId, DownloadKind.MATERIAL, topic.name, 380_000)
    }
    override suspend fun downloadQuestionSet(setId: String) = add(setId, DownloadKind.QUESTION_SET, t("Question set", "প্রশ্ন সেট"), 120_000)
    override suspend fun downloadDeck(deckId: String) = add(deckId, DownloadKind.FLASHCARD_DECK, SampleRevision.decks.firstOrNull { it.first == deckId }?.second ?: t("Flashcards", "ফ্ল্যাশকার্ড"), 40_000)
    override suspend fun remove(downloadId: String) { store.downloads.update { list -> list.filter { it.id != downloadId } } }
    override suspend fun clearCache() {}
    override fun requestSync() { store.pendingChanges.value = 0 }
}
