package app.prostuti.core.mockbackend

import app.prostuti.core.common.AppError
import app.prostuti.core.common.AppResult
import app.prostuti.core.common.asSuccess
import app.prostuti.core.domain.exam.NegativeMarking
import app.prostuti.core.domain.exam.OptionShuffler
import app.prostuti.core.domain.practice.PracticeEvaluator
import app.prostuti.core.domain.repository.*
import app.prostuti.core.domain.revision.SpacedRepetition
import app.prostuti.core.model.*
import kotlinx.coroutines.flow.*
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Development OTP; shown on the sign-in screen in mock builds only. */
const val MOCK_OTP = "123456"

@Singleton
class MockAuthRepository @Inject constructor(private val store: MockStore) : AuthRepository {
    override val session: Flow<SessionState> = combine(store.signedInUser, store.onboarded) { user, onboarded ->
        if (user == null) SessionState.SignedOut else SessionState.SignedIn(user, onboarded)
    }

    override suspend fun requestOtp(phone: String): AppResult<OtpChallenge> {
        latency()
        if (!Regex("^(?:\\+?88)?01[3-9]\\d{8}$").matches(phone)) return AppResult.Failure(AppError.Validation("Enter a valid Bangladeshi mobile number", mapOf("phone" to "invalid")))
        return OtpChallenge("ch-${store.newId()}", phone, Instant.now().plusSeconds(30)).asSuccess()
    }

    override suspend fun verifyOtp(challengeId: String, code: String): AppResult<AuthSession> {
        latency(500)
        if (code != MOCK_OTP) return AppResult.Failure(AppError.Validation("The code is incorrect", mapOf("code" to "invalid")))
        return signIn()
    }

    override suspend fun signInWithEmail(email: String, password: String): AppResult<AuthSession> {
        latency(500)
        if (password.length < 8) return AppResult.Failure(AppError.Validation("Email or password is incorrect"))
        return signIn()
    }

    override suspend fun registerWithEmail(name: String, email: String, password: String): AppResult<AuthSession> {
        latency(500)
        return signIn(name)
    }

    override suspend fun requestPasswordReset(email: String): AppResult<Unit> { latency(); return Unit.asSuccess() }

    override suspend fun signOut() {
        store.signedInUser.value = null
        store.onboarded.value = false
        store.profile.value = null
    }

    private fun signIn(name: String = ""): AppResult<AuthSession> {
        val isNew = store.profile.value == null
        store.signedInUser.value = "student-1"
        if (isNew && name.isNotBlank()) store.profile.value = null
        return AuthSession("student-1", isNewUser = !store.onboarded.value).asSuccess()
    }
}

@Singleton
class MockProfileRepository @Inject constructor(private val store: MockStore) : ProfileRepository {
    override val profile: Flow<StudentProfile?> = store.profile

    override suspend fun refresh(): AppResult<StudentProfile> {
        latency()
        return store.profile.value?.asSuccess() ?: AppResult.Failure(AppError.NotFound)
    }

    override suspend fun savePreferences(preferences: PreparationPreferences): AppResult<StudentProfile> {
        latency()
        val p = StudentProfile(
            id = "student-1", fullName = preferences.fullName, phone = "01700000000", email = null, avatarUrl = null,
            language = preferences.language, targets = preferences.targets, dailyStudyMinutes = preferences.dailyStudyMinutes,
            level = preferences.level, strongSubjectIds = preferences.strongSubjectIds, weakSubjectIds = preferences.weakSubjectIds,
            onboardingCompleted = store.onboarded.value,
        )
        store.profile.value = p
        return p.asSuccess()
    }

    override suspend fun previewInitialPlan(preferences: PreparationPreferences): AppResult<List<PlanTask>> {
        latency(700)
        return SamplePlan.days(store, preferences.dailyStudyMinutes, preferences.weakSubjectIds).take(3).flatMap { it.tasks }.asSuccess()
    }

    override suspend fun completeOnboarding(): AppResult<Unit> {
        latency()
        store.onboarded.value = true
        store.profile.value = store.profile.value?.copy(onboardingCompleted = true)
        return Unit.asSuccess()
    }
}

@Singleton
class MockExamDirectoryRepository @Inject constructor(private val store: MockStore) : ExamRepository {
    private fun summary(e: SampleContent.SampleExam): ExamSummary {
        val targets = store.profile.value?.targets.orEmpty().map { it.examId }.toSet()
        return e.summary.copy(isBookmarked = e.summary.id in store.bookmarkedExams, isTarget = e.summary.id in targets || (targets.isEmpty() && e.summary.isTarget))
    }

    override suspend fun exams(filter: ExamFilter, cursor: String?): AppResult<Page<ExamSummary>> {
        latency()
        val q = filter.query.trim().lowercase()
        val items = SampleContent.exams.map(::summary).filter {
            (q.isEmpty() || it.name.en.lowercase().contains(q) || it.name.bn.orEmpty().contains(q) || it.shortName.lowercase().contains(q)) &&
                (filter.category == null || it.category == filter.category) &&
                (!filter.bookmarkedOnly || it.isBookmarked)
        }
        return Page(items, null).asSuccess()
    }

    override suspend fun exam(examId: String): AppResult<ExamDetail> {
        latency()
        val e = SampleContent.exams.firstOrNull { it.summary.id == examId } ?: return AppResult.Failure(AppError.NotFound)
        return e.detail.copy(summary = summary(e)).asSuccess()
    }

    override suspend fun setBookmarked(examId: String, bookmarked: Boolean): AppResult<Unit> {
        if (bookmarked) store.bookmarkedExams += examId else store.bookmarkedExams -= examId
        return Unit.asSuccess()
    }

    override suspend fun compare(examIds: List<String>): AppResult<SyllabusComparison> {
        latency()
        val exams = SampleContent.exams.filter { it.summary.id in examIds }
        val subjectNames = linkedMapOf<String, LocalizedText>()
        exams.forEach { e -> e.detail.stages.firstOrNull()?.subjects?.forEach { subjectNames.putIfAbsent(it.subjectId, it.subjectName) } }
        val rows = subjectNames.map { (sid, name) ->
            SyllabusComparisonRow(name, exams.associate { e -> e.summary.id to e.detail.stages.firstOrNull()?.subjects?.firstOrNull { it.subjectId == sid }?.marks })
        }
        val shared = SampleContent.topics.count { t -> examIds.all { it in t.exams } }
        return SyllabusComparison(exams.map(::summary), rows, shared).asSuccess()
    }

    override suspend fun setTargets(targets: List<TargetExam>): AppResult<Unit> {
        store.profile.value = store.profile.value?.copy(targets = targets)
        return Unit.asSuccess()
    }

    override suspend fun announcements(): AppResult<List<Announcement>> = SamplePlan.announcements().asSuccess()
}

@Singleton
class MockLearningRepository @Inject constructor(private val store: MockStore) : LearningRepository {
    private fun topicSummary(t: SampleContent.SampleTopic) = TopicSummary(
        t.id, t.name, t.minutes, SampleQuestions.all.count { it.topicId == t.id }, t.id in store.completedTopics,
        store.mastery(t.id), t.exams, store.downloads.value.any { it.id == t.id && it.state == DownloadState.COMPLETED },
    )

    override suspend fun subjects(examId: String): AppResult<List<Subject>> {
        latency()
        val exam = SampleContent.exams.firstOrNull { it.summary.id == examId } ?: return AppResult.Failure(AppError.NotFound)
        val stageSubjects = exam.detail.stages.firstOrNull()?.subjects.orEmpty()
        return stageSubjects.map { sm ->
            val topics = SampleContent.topics.filter { it.subjectId == sm.subjectId }
            val mastery = topics.map { store.mastery(it.id).value }.average().takeIf { !it.isNaN() }?.toFloat() ?: 0f
            Subject(
                sm.subjectId, sm.subjectName, sm.marks, SampleContent.chapters.count { it.subjectId == sm.subjectId },
                topics.size, topics.count { it.id in store.completedTopics }, Mastery(mastery),
            )
        }.asSuccess()
    }

    override suspend fun chapters(subjectId: String): AppResult<List<Chapter>> {
        latency()
        return SampleContent.chapters.filter { it.subjectId == subjectId }.map { c ->
            Chapter(c.id, c.subjectId, c.name, SampleContent.topics.filter { it.chapterId == c.id }.map(::topicSummary))
        }.asSuccess()
    }

    override suspend fun topic(topicId: String): AppResult<TopicDetail> {
        latency()
        val t = SampleContent.topics.firstOrNull { it.id == topicId } ?: return AppResult.Failure(AppError.NotFound)
        val chapter = SampleContent.chapters.first { it.id == t.chapterId }
        val prereqs = SampleContent.topics.filter { it.chapterId == t.chapterId && it.id != t.id }.take(1).map(::topicSummary)
        val materials = listOf(
            LearningMaterialSummary("m-${t.id}", t.id, t("Study notes", "স্টাডি নোট"), MaterialType.ARTICLE, t.minutes, 40_000, false, topicSummary(t).isAvailableOffline),
            LearningMaterialSummary("v-${t.id}", t.id, t("Video lesson", "ভিডিও লেসন"), MaterialType.VIDEO, 12, 48_000_000, true, false),
        )
        return TopicDetail(topicSummary(t), SampleContent.subjects[t.subjectId] ?: t(t.subjectId), chapter.name, prereqs, materials, store.topicNotes[topicId], topicId in store.bookmarkedTopics).asSuccess()
    }

    override suspend fun material(materialId: String): AppResult<LearningMaterial> {
        latency()
        val topicId = materialId.substringAfter('-')
        val t = SampleContent.topics.firstOrNull { it.id == topicId } ?: return AppResult.Failure(AppError.NotFound)
        val isVideo = materialId.startsWith("v-")
        val blocks = if (isVideo) listOf(
            ContentBlock.Video("https://example.com/lessons/$topicId.mp4", t.name, 720, null),
            ContentBlock.Paragraph(t("Video lessons stream on demand and are part of Prostuti Plus.", "ভিডিও লেসন অন-ডিমান্ড স্ট্রিম হয় এবং প্রস্তুতি প্লাস-এর অংশ।")),
        ) else SampleMaterials.blocksFor(topicId)
        return LearningMaterial(
            LearningMaterialSummary(materialId, topicId, t.name, if (isVideo) MaterialType.VIDEO else MaterialType.ARTICLE, t.minutes, null, isVideo, !isVideo),
            blocks, listOf(SampleContent.bpsc), Instant.now().minusSeconds(86_400 * 5),
        ).asSuccess()
    }

    override suspend fun setTopicCompleted(topicId: String, completed: Boolean): AppResult<Unit> {
        if (completed) store.completedTopics += topicId else store.completedTopics -= topicId
        return Unit.asSuccess()
    }

    override suspend fun setTopicBookmarked(topicId: String, bookmarked: Boolean): AppResult<Unit> {
        if (bookmarked) store.bookmarkedTopics += topicId else store.bookmarkedTopics -= topicId
        return Unit.asSuccess()
    }

    override suspend fun saveTopicNote(topicId: String, note: String): AppResult<Unit> {
        store.topicNotes[topicId] = note
        return Unit.asSuccess()
    }
}

@Singleton
class MockQuestionRepository @Inject constructor(private val store: MockStore) : QuestionRepository {
    private fun decorate(q: Question) = q.copy(isBookmarked = q.id in store.bookmarkedQuestions, note = store.questionNotes[q.id])

    override suspend fun questions(filter: QuestionFilter, cursor: String?): AppResult<Page<Question>> {
        latency()
        val query = filter.query.trim().lowercase()
        val list = SampleQuestions.all.map(::decorate).filter { q ->
            val topic = SampleContent.topics.first { it.id == q.topicId }
            (query.isEmpty() || q.stem.en.lowercase().contains(query) || q.stem.bn.orEmpty().contains(query)) &&
                (filter.examId == null || filter.examId in topic.exams) &&
                (filter.subjectId == null || topic.subjectId == filter.subjectId) &&
                (filter.topicId == null || q.topicId == filter.topicId) &&
                (filter.difficulty == null || q.difficulty == filter.difficulty) &&
                (filter.type == null || q.type == filter.type) &&
                (filter.provenance == null || q.provenance == filter.provenance) &&
                (!filter.previousYearOnly || q.provenance != QuestionProvenance.COMMUNITY) &&
                (!filter.bookmarkedOnly || q.isBookmarked)
        }
        val page = cursor?.toIntOrNull() ?: 0
        val size = 12
        val slice = list.drop(page * size).take(size)
        return Page(slice, if ((page + 1) * size < list.size) "${page + 1}" else null).asSuccess()
    }

    override suspend fun question(questionId: String): AppResult<Question> {
        latency(200)
        return SampleQuestions.byId(questionId)?.let(::decorate)?.asSuccess() ?: AppResult.Failure(AppError.NotFound)
    }

    override suspend fun similar(questionId: String): AppResult<List<Question>> {
        val q = SampleQuestions.byId(questionId) ?: return AppResult.Failure(AppError.NotFound)
        val subject = SampleContent.topics.first { it.id == q.topicId }.subjectId
        return SampleQuestions.objective().filter { it.id != q.id && SampleContent.topics.first { t -> t.id == it.topicId }.subjectId == subject }.take(3).asSuccess()
    }

    override suspend fun setBookmarked(questionId: String, bookmarked: Boolean): AppResult<Unit> {
        if (bookmarked) store.bookmarkedQuestions += questionId else store.bookmarkedQuestions -= questionId
        return Unit.asSuccess()
    }

    override suspend fun saveNote(questionId: String, note: String): AppResult<Unit> { store.questionNotes[questionId] = note; return Unit.asSuccess() }
    override suspend fun report(questionId: String, reason: QuestionReportReason, details: String): AppResult<Unit> { latency(); return Unit.asSuccess() }
    override fun wrongAnswers(): Flow<List<WrongAnswerEntry>> = store.wrongAnswers

    override suspend fun markWrongAnswerResolved(questionId: String): AppResult<Unit> {
        store.wrongAnswers.update { list -> list.map { if (it.question.id == questionId) it.copy(resolved = true) else it } }
        return Unit.asSuccess()
    }

    override suspend fun collections(): AppResult<List<QuestionCollection>> =
        store.collections.map { it.copy(questionCount = store.collectionItems[it.id]?.size ?: 0) }.asSuccess()

    override suspend fun createCollection(name: String): AppResult<QuestionCollection> {
        val c = QuestionCollection(store.newId(), name, 0, Instant.now())
        store.collections += c
        return c.asSuccess()
    }

    override suspend fun addToCollection(collectionId: String, questionId: String): AppResult<Unit> {
        store.collectionItems.getOrPut(collectionId) { mutableSetOf() } += questionId
        return Unit.asSuccess()
    }
}

@Singleton
class MockPracticeRepository @Inject constructor(private val store: MockStore) : PracticeRepository {
    override suspend fun start(config: PracticeConfig): AppResult<PracticeSession> {
        latency(500)
        val pool = SampleQuestions.objective().filter { q ->
            val topic = SampleContent.topics.first { it.id == q.topicId }
            (config.topicIds.isEmpty() || q.topicId in config.topicIds) &&
                (config.subjectIds.isEmpty() || topic.subjectId in config.subjectIds) &&
                (config.difficulty == null || q.difficulty == config.difficulty)
        }
        val chosen = when (config.mode) {
            PracticeMode.WRONG_ANSWERS -> store.wrongAnswers.value.filter { !it.resolved }.map { it.question }
            PracticeMode.WEAK_TOPICS -> pool.sortedBy { store.mastery(it.topicId).value }
            PracticeMode.PREVIOUS_YEAR -> pool.filter { it.provenance == QuestionProvenance.OFFICIAL }
            else -> pool.shuffled()
        }.take(config.questionCount)
        if (chosen.isEmpty()) return AppResult.Failure(AppError.Validation("No questions match these filters yet."))
        return PracticeSession("ps-${store.newId()}", config, chosen, isOffline = false).asSuccess()
    }

    override suspend fun submit(session: PracticeSession, answers: List<AnswerRecord>): AppResult<PracticeResult> {
        latency(600)
        val byId = answers.associateBy { it.questionId }
        session.questions.forEach { q ->
            val selected = byId[q.id]?.selectedOptionIds.orEmpty()
            if (selected.isEmpty()) return@forEach
            val correct = NegativeMarking.isAnswerCorrect(selected, q.solution?.correctOptionIds.orEmpty())
            store.record(q.id, correct)
            if (!correct) store.wrongAnswers.update { list ->
                val existing = list.firstOrNull { it.question.id == q.id }
                listOf(WrongAnswerEntry(q, selected, (existing?.timesWrong ?: 0) + 1, Instant.now(), false)) + list.filter { it.question.id != q.id }
            }
        }
        store.studiedTodayMinutes += (answers.sumOf { it.timeSpent.seconds } / 60).toInt()
        return PracticeEvaluator.evaluate(session, answers).copy(isServerVerified = true).asSuccess()
    }

    override suspend fun recommendedSets(): AppResult<List<PracticeSetSummary>> = SamplePlan.sets(store).asSuccess()
    override suspend fun dailyChallenge(): AppResult<PracticeConfig> = PracticeConfig(PracticeMode.DAILY_CHALLENGE, questionCount = 10, durationMinutes = 8).asSuccess()
}

@Singleton
class MockMockExamRepository @Inject constructor(private val store: MockStore) : MockExamRepository {
    private val sectionPlan = listOf(
        Triple("sec-bangla", t("Bangla", "বাংলা"), listOf("bangla")),
        Triple("sec-english", t("English", "ইংরেজি"), listOf("english")),
        Triple("sec-gk", t("Bangladesh & international", "বাংলাদেশ ও আন্তর্জাতিক"), listOf("bd-affairs", "intl-affairs", "geography", "ethics")),
        Triple("sec-math", t("Math & mental ability", "গণিত ও মানসিক দক্ষতা"), listOf("math", "mental")),
        Triple("sec-sci", t("Science & ICT", "বিজ্ঞান ও আইসিটি"), listOf("science", "ict")),
    )

    private fun sectionQuestions(subjects: List<String>) =
        SampleQuestions.objective().filter { q -> SampleContent.topics.first { it.id == q.topicId }.subjectId in subjects }

    private val mocks: List<MockExamSummary> by lazy {
        val sections = sectionPlan.map { (id, name, subs) -> MockSection(id, name, sectionQuestions(subs).size, 1.0, 0.5, null) }
        listOf(
            MockExamSummary(
                "mock-bcs-1", t("BCS preliminary model test 01", "বিসিএস প্রিলিমিনারি মডেল টেস্ট ০১"), "bcs", "BCS", true,
                durationMinutes = 25, totalMarks = sections.sumOf { it.questionCount }.toDouble(), questionCount = sections.sumOf { it.questionCount },
                sections = sections,
                rules = MockRules(allowSectionSwitching = true, revealAnswersDuringExam = false, shuffleQuestions = false, shuffleOptions = true, autoSubmitOnTimeout = true),
                attemptCount = 0, bestScore = null, isPremium = false, availableFrom = null,
            ),
            MockExamSummary(
                "mock-math-1", t("Math sectional test", "গণিত সেকশনাল টেস্ট"), "bcs", "BCS", false,
                durationMinutes = 8, totalMarks = sections[3].questionCount.toDouble(), questionCount = sections[3].questionCount,
                sections = listOf(sections[3]),
                rules = MockRules(true, false, true, true, true), attemptCount = 0, bestScore = null, isPremium = false, availableFrom = null,
            ),
            MockExamSummary(
                "mock-ntrca-1", t("NTRCA full model test", "এনটিআরসিএ পূর্ণাঙ্গ মডেল টেস্ট"), "ntrca", "NTRCA", true,
                durationMinutes = 60, totalMarks = 100.0, questionCount = 100,
                sections = sections.map { it.copy(negativeMarkPerWrong = 0.25) },
                rules = MockRules(true, false, true, true, true), attemptCount = 0, bestScore = null, isPremium = true, availableFrom = null,
            ),
        )
    }

    private fun withStats(m: MockExamSummary): MockExamSummary {
        val mine = store.results.values.filter { it.mock.id == m.id }
        return m.copy(attemptCount = mine.size, bestScore = mine.maxOfOrNull { it.score })
    }

    override suspend fun mocks(examId: String?): AppResult<List<MockExamSummary>> {
        latency()
        return mocks.filter { examId == null || it.examId == examId }.map(::withStats).asSuccess()
    }

    override suspend fun mock(mockId: String): AppResult<MockExamSummary> =
        mocks.firstOrNull { it.id == mockId }?.let(::withStats)?.asSuccess() ?: AppResult.Failure(AppError.NotFound)

    override suspend fun startAttempt(mockId: String): AppResult<MockAttempt> {
        latency(600)
        val mock = mocks.firstOrNull { it.id == mockId } ?: return AppResult.Failure(AppError.NotFound)
        if (mock.isPremium) return AppResult.Failure(AppError.PremiumRequired("FULL_MOCKS"))
        store.attempts.values.firstOrNull { it.mock.id == mockId && !store.results.containsKey(it.attemptId) }?.let { return resume(it) }
        val attemptId = "att-${store.newId()}"
        val now = Instant.now()
        val sections = mock.sections.map { section ->
            val subs = sectionPlan.first { it.first == section.id }.third
            // Server strips solutions from questions during an active attempt.
            val qs = OptionShuffler.shuffle(sectionQuestions(subs), attemptId, mock.rules.shuffleQuestions, mock.rules.shuffleOptions)
                .map { it.copy(solution = null) }
            AttemptSection(section, qs)
        }
        val attempt = MockAttempt(attemptId, mock, sections, now, now.plus(Duration.ofMinutes(mock.durationMinutes.toLong())), emptyMap(), emptySet())
        store.attempts[attemptId] = attempt
        return attempt.asSuccess()
    }

    private fun resume(a: MockAttempt) = a.copy(serverNow = Instant.now()).asSuccess()

    override suspend fun resumeAttempt(attemptId: String): AppResult<MockAttempt> {
        latency()
        return store.attempts[attemptId]?.let(::resume) ?: AppResult.Failure(AppError.NotFound)
    }

    override suspend fun saveAnswers(attemptId: String, answers: Map<String, Set<String>>, markedForReview: Set<String>): AppResult<Unit> {
        latency(150)
        val a = store.attempts[attemptId] ?: return AppResult.Failure(AppError.NotFound)
        if (Instant.now().isAfter(a.endsAt.plusSeconds(5))) return AppResult.Failure(AppError.Conflict("The exam time is over."))
        store.attempts[attemptId] = a.copy(savedAnswers = answers, markedForReview = markedForReview)
        return Unit.asSuccess()
    }

    /** Scoring happens "server-side" here, mirroring what the backend does. */
    override suspend fun submit(attemptId: String, answers: Map<String, Set<String>>): AppResult<MockResult> {
        latency(900)
        store.results[attemptId]?.let { return it.asSuccess() } // idempotent
        val a = store.attempts[attemptId] ?: return AppResult.Failure(AppError.NotFound)
        val now = Instant.now()
        val accepted = if (now.isAfter(a.endsAt.plusSeconds(30))) a.savedAnswers else answers
        val full = a.sections.map { s -> s to s.questions.map { q -> SampleQuestions.byId(q.id)!!.let { orig -> q.copy(solution = orig.solution) } } }
        val sectionResults = full.map { (s, qs) ->
            var c = 0; var w = 0; var sk = 0
            qs.forEach { q ->
                val sel = accepted[q.id].orEmpty()
                when {
                    sel.isEmpty() -> sk++
                    NegativeMarking.isAnswerCorrect(sel, q.solution!!.correctOptionIds) -> c++
                    else -> w++
                }
                if (sel.isNotEmpty()) store.record(q.id, NegativeMarking.isAnswerCorrect(sel, q.solution!!.correctOptionIds))
            }
            val score = NegativeMarking.score(c, w, sk, s.section.marksPerQuestion, s.section.negativeMarkPerWrong)
            SectionResult(s.section, c, w, sk, score.net, score.negative, qs.size * s.section.marksPerQuestion)
        }
        val weakest = sectionResults.minByOrNull { if (it.maxScore == 0.0) 1.0 else it.score / it.maxScore }
        val result = MockResult(
            attemptId = attemptId, mock = a.mock, submittedAt = now,
            timeTaken = Duration.between(a.endsAt.minus(Duration.ofMinutes(a.mock.durationMinutes.toLong())), now).coerceAtMost(Duration.ofMinutes(a.mock.durationMinutes.toLong())),
            score = sectionResults.sumOf { it.score }, maxScore = sectionResults.sumOf { it.maxScore },
            negativeMarks = sectionResults.sumOf { it.negativeMarks }, sectionResults = sectionResults,
            percentile = 72.5f, rank = 812, totalCandidates = 2954, passMarks = sectionResults.sumOf { it.maxScore } * 0.5,
            questions = full.flatMap { it.second }, answers = accepted,
            recommendations = listOfNotNull(
                weakest?.let {
                    Recommendation("rec-sec", t("Strengthen ${it.section.name.en}", "${it.section.name.bn} আরও শক্ত করুন"),
                        t("Lowest section score: ${"%.1f".format(it.score)} of ${it.maxScore.toInt()}.", "সবচেয়ে কম নম্বর এই সেকশনে: ${it.maxScore.toInt()} এর মধ্যে ${"%.1f".format(it.score)}।"),
                        RecommendedAction.ReviewWrongAnswers)
                },
                sectionResults.sumOf { it.negativeMarks }.takeIf { it >= 2 }?.let { neg ->
                    Recommendation("rec-neg", t("Guess less when unsure", "নিশ্চিত না হলে অনুমান কম করুন"),
                        t("You lost $neg marks to negative marking. Skip when you can't eliminate at least two options.", "নেগেটিভ মার্কিংয়ে $neg নম্বর কাটা গেছে। অন্তত দুটি অপশন বাদ দিতে না পারলে উত্তর না দেওয়াই ভালো।"),
                        RecommendedAction.PracticeTopic("t-interest"))
                },
            ),
        )
        store.results[attemptId] = result
        return result.asSuccess()
    }

    override suspend fun result(attemptId: String): AppResult<MockResult> =
        store.results[attemptId]?.asSuccess() ?: AppResult.Failure(AppError.NotFound)

    override suspend fun history(): AppResult<List<MockAttemptHistoryItem>> {
        latency()
        val mine = store.results.values.sortedByDescending { it.submittedAt }.map {
            MockAttemptHistoryItem(it.attemptId, it.mock.title, it.submittedAt, it.score, it.maxScore, it.negativeMarks, it.percentile)
        }
        return (mine + SamplePlan.pastAttempts()).asSuccess()
    }
}
