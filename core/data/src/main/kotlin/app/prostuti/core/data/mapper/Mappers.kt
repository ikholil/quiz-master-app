package app.prostuti.core.data.mapper

import app.prostuti.core.model.*
import app.prostuti.core.network.dto.*
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/* DTO → domain mapping. Unknown enum values from a newer backend degrade gracefully. */

internal inline fun <reified E : Enum<E>> String.toEnum(default: E): E =
    runCatching { enumValueOf<E>(this.uppercase()) }.getOrDefault(default)

internal fun String.instant(): Instant = Instant.parse(this)
internal fun String.date(): LocalDate = LocalDate.parse(this.take(10))

fun LocalizedDto.toModel() = LocalizedText(en, bn)
fun LocalizedText.toDto() = LocalizedDto(en, bn)
fun SourceDto.toModel() = SourceReference(title, publisher, url, publishedAt?.instant())

fun RecommendationDto.toModel(): Recommendation = Recommendation(
    id = id,
    title = title.toModel(),
    reason = reason.toModel(),
    action = when (actionType.uppercase()) {
        "PRACTICE_TOPIC" -> RecommendedAction.PracticeTopic(targetId.orEmpty())
        "STUDY_TOPIC" -> RecommendedAction.StudyTopic(targetId.orEmpty())
        "TAKE_MOCK" -> RecommendedAction.TakeMock(targetId.orEmpty())
        "OPEN_PRACTICE_SET" -> RecommendedAction.OpenPracticeSet(targetId.orEmpty())
        "REVIEW_FLASHCARDS" -> RecommendedAction.ReviewFlashcards
        else -> RecommendedAction.ReviewWrongAnswers
    },
)

fun TokensDto.toModel() = AuthTokens(accessToken, refreshToken, accessExpiresAt.instant())
fun OtpChallengeDto.toModel() = OtpChallenge(challengeId, phone, resendAvailableAt.instant(), codeLength)

fun TargetExamDto.toModel() = TargetExam(examId, examName.toModel(), examDate?.date(), isPrimary)
fun TargetExam.toDto() = TargetExamDto(examId, examName.toDto(), examDate?.toString(), isPrimary)

fun ProfileDto.toModel() = StudentProfile(
    id = id,
    fullName = fullName,
    phone = phone,
    email = email,
    avatarUrl = avatarUrl,
    language = if (language.startsWith("en")) AppLanguage.ENGLISH else AppLanguage.BENGALI,
    targets = targets.map { it.toModel() },
    dailyStudyMinutes = dailyStudyMinutes,
    level = level.toEnum(PreparationLevel.BEGINNER),
    strongSubjectIds = strongSubjectIds,
    weakSubjectIds = weakSubjectIds,
    onboardingCompleted = onboardingCompleted,
)

fun PreparationPreferences.toDto() = PreferencesDto(
    fullName = fullName,
    language = language.tag,
    targets = targets.map { it.toDto() },
    dailyStudyMinutes = dailyStudyMinutes,
    level = level.name,
    strongSubjectIds = strongSubjectIds,
    weakSubjectIds = weakSubjectIds,
)

fun PrivacyDto.toModel() = PrivacySettings(showOnLeaderboards, shareProgressWithGroups, personalizedRecommendations, marketingNotifications)
fun PrivacySettings.toDto() = PrivacyDto(showOnLeaderboards, shareProgressWithGroups, personalizedRecommendations, marketingNotifications)

// ---- Exams ----
fun DeadlineDto.toModel() = ExamDeadline(label.toModel(), date.date())

fun ExamSummaryDto.toModel() = ExamSummary(
    id, name.toModel(), shortName, category.toEnum(ExamCategory.OTHER), authority.toModel(),
    nextDeadline?.toModel(), isBookmarked, isTarget,
)

fun ExamStageDto.toModel() = ExamStage(
    id, name.toModel(), format.toModel(), totalMarks, durationMinutes, negativeMarkPerWrong, passMarks,
    subjects.map { SubjectMarks(it.subjectId, it.subjectName.toModel(), it.marks, it.questionCount) },
)

fun ExamDetailDto.toModel() = ExamDetail(
    summary = summary.toModel(),
    description = description.toModel(),
    eligibility = eligibility.map { it.toModel() },
    stages = stages.map { it.toModel() },
    deadlines = deadlines.map { it.toModel() },
    notices = notices.map { ExamNotice(it.id, it.title.toModel(), it.source.toModel()) },
    previousPaperCount = previousPaperCount,
    officialSource = officialSource.toModel(),
    lastVerifiedAt = lastVerifiedAt.instant(),
)

fun ComparisonDto.toModel() = SyllabusComparison(
    exams.map { it.toModel() },
    rows.map { SyllabusComparisonRow(it.subjectName.toModel(), it.marksByExam) },
    sharedTopicCount,
)

fun AnnouncementDto.toModel() = Announcement(id, title.toModel(), body.toModel(), source?.toModel(), publishedAt.instant())

// ---- Learning ----
fun SubjectDto.toModel() = Subject(id, name.toModel(), marks, chapterCount, topicCount, completedTopicCount, Mastery(mastery.coerceIn(0f, 1f)))

fun TopicSummaryDto.toModel(offline: Boolean = false) = TopicSummary(
    id, name.toModel(), estimatedMinutes, questionCount, isCompleted, Mastery(mastery.coerceIn(0f, 1f)), sharedWithExamIds, offline,
)

fun ChapterDto.toModel(offlineIds: Set<String> = emptySet()) =
    Chapter(id, subjectId, name.toModel(), topics.map { it.toModel(it.id in offlineIds) })

fun MaterialSummaryDto.toModel(downloaded: Boolean = false) = LearningMaterialSummary(
    id, topicId, title.toModel(), type.toEnum(MaterialType.ARTICLE), durationMinutes, sizeBytes, isPremium, downloaded,
)

fun TopicDetailDto.toModel(offline: Boolean = false) = TopicDetail(
    summary = summary.toModel(offline),
    subjectName = subjectName.toModel(),
    chapterName = chapterName.toModel(),
    prerequisites = prerequisites.map { it.toModel() },
    materials = materials.map { it.toModel(offline) },
    note = note,
    isBookmarked = isBookmarked,
)

fun ContentBlockDto.toModel(): ContentBlock? {
    val empty = LocalizedText.Empty
    return when (type.lowercase()) {
        "heading" -> ContentBlock.Heading(text?.toModel() ?: empty, level ?: 2)
        "paragraph" -> ContentBlock.Paragraph(text?.toModel() ?: empty)
        "bullets" -> ContentBlock.Bullets(items.orEmpty().map { it.toModel() })
        "callout" -> ContentBlock.Callout((kind ?: "TIP").toEnum(CalloutKind.TIP), text?.toModel() ?: empty)
        "image" -> url?.let { ContentBlock.Image(it, caption?.toModel(), altText?.toModel() ?: empty) }
        "table" -> ContentBlock.Table(header.orEmpty().map { it.toModel() }, rows.orEmpty().map { r -> r.map { it.toModel() } })
        "formula" -> expression?.let { ContentBlock.Formula(it, text?.toModel() ?: empty) }
        "audio" -> url?.let { ContentBlock.Audio(it, text?.toModel() ?: empty, durationSeconds ?: 0) }
        "video" -> url?.let { ContentBlock.Video(it, text?.toModel() ?: empty, durationSeconds ?: 0, thumbnailUrl) }
        "pdf" -> url?.let { ContentBlock.Pdf(it, text?.toModel() ?: empty, sizeBytes ?: 0) }
        else -> null // Unknown block types from a newer backend are skipped, not crashed on.
    }
}

fun MaterialDto.toModel(downloaded: Boolean = false) = LearningMaterial(
    summary.toModel(downloaded), blocks.mapNotNull { it.toModel() }, sources.map { it.toModel() }, updatedAt.instant(),
)

// ---- Questions ----
fun QuestionDto.toModel() = Question(
    id = id,
    type = type.toEnum(QuestionType.MCQ),
    stem = stem.toModel(),
    options = options.map { QuestionOption(it.id, it.text.toModel(), it.imageUrl) },
    passage = passage?.let { Passage(it.id, it.title?.toModel(), it.text.toModel()) },
    imageUrl = imageUrl,
    formula = formula,
    difficulty = difficulty.toEnum(Difficulty.MEDIUM),
    provenance = provenance.toEnum(QuestionProvenance.COMMUNITY),
    topicId = topicId,
    topicName = topicName.toModel(),
    subjectName = subjectName.toModel(),
    examReferences = examReferences.map { ExamReference(it.examName.toModel(), it.year, it.stage) },
    sources = sources.map { it.toModel() },
    solution = solution?.let { s ->
        QuestionSolution(
            s.correctOptionIds.toSet(), s.explanation.toModel(),
            s.optionExplanations.mapValues { it.value.toModel() }, s.modelAnswer?.toModel(),
        )
    },
    isBookmarked = isBookmarked,
    note = note,
)

fun CollectionDto.toModel() = QuestionCollection(id, name, questionCount, updatedAt.instant())

fun WrongAnswerDto.toModel() = WrongAnswerEntry(question.toModel(), selectedOptionIds.toSet(), timesWrong, lastWrongAt.instant(), resolved)

// ---- Practice ----
fun PracticeConfig.toDto() = PracticeConfigDto(
    mode.name, subjectIds, topicIds, questionCount, difficulty?.name, durationMinutes, instantFeedback, setId,
)

fun PracticeConfigDto.toModel() = PracticeConfig(
    mode.toEnum(PracticeMode.CUSTOM), subjectIds, topicIds, questionCount,
    difficulty?.toEnum(Difficulty.MEDIUM), durationMinutes, instantFeedback, setId = setId,
)

fun PracticeSessionDto.toModel() = PracticeSession(id, config.toModel(), questions.map { it.toModel() }, isOffline = false)

fun AnswerRecord.toDto() = AnswerDto(questionId, selectedOptionIds.toList(), timeSpent.toMillis())
fun AnswerDto.toModel() = AnswerRecord(questionId, selectedOptionIds.toSet(), Duration.ofMillis(timeSpentMs))

fun PracticeResultDto.toModel() = PracticeResult(
    sessionId = sessionId,
    mode = mode.toEnum(PracticeMode.CUSTOM),
    correct = correct,
    incorrect = incorrect,
    skipped = skipped,
    score = score,
    maxScore = maxScore,
    timeSpent = Duration.ofMillis(timeSpentMs),
    topicOutcomes = topicOutcomes.map { TopicOutcome(it.topicId, it.topicName.toModel(), it.correct, it.total) },
    questions = questions.map { it.toModel() },
    answers = answers.associate { it.questionId to it.toModel() },
    nextAction = nextAction?.toModel(),
    isServerVerified = true,
)

fun PracticeSetDto.toModel(downloaded: Boolean = false) =
    PracticeSetSummary(id, title.toModel(), questionCount, estimatedMinutes, reason.toModel(), downloaded)

// ---- Mock exams ----
fun MockSectionDto.toModel() = MockSection(id, name.toModel(), questionCount, marksPerQuestion, negativeMarkPerWrong, durationMinutes)

fun MockSummaryDto.toModel() = MockExamSummary(
    id = id,
    title = title.toModel(),
    examId = examId,
    examShortName = examShortName,
    isFullLength = isFullLength,
    durationMinutes = durationMinutes,
    totalMarks = totalMarks,
    questionCount = questionCount,
    sections = sections.map { it.toModel() },
    rules = MockRules(rules.allowSectionSwitching, rules.revealAnswersDuringExam, rules.shuffleQuestions, rules.shuffleOptions, rules.autoSubmitOnTimeout),
    attemptCount = attemptCount,
    bestScore = bestScore,
    isPremium = isPremium,
    availableFrom = availableFrom?.instant(),
)

fun AttemptDto.toModel() = MockAttempt(
    attemptId = attemptId,
    mock = mock.toModel(),
    sections = sections.map { AttemptSection(it.section.toModel(), it.questions.map { q -> q.toModel() }) },
    serverNow = serverNow.instant(),
    endsAt = endsAt.instant(),
    savedAnswers = savedAnswers.associate { it.questionId to it.selectedOptionIds.toSet() },
    markedForReview = markedForReview.toSet(),
)

fun Map<String, Set<String>>.toSavedAnswers() = map { (q, opts) -> SavedAnswerDto(q, opts.toList()) }

fun MockResultDto.toModel() = MockResult(
    attemptId = attemptId,
    mock = mock.toModel(),
    submittedAt = submittedAt.instant(),
    timeTaken = Duration.ofMillis(timeTakenMs),
    score = score,
    maxScore = maxScore,
    negativeMarks = negativeMarks,
    sectionResults = sectionResults.map {
        SectionResult(it.section.toModel(), it.correct, it.incorrect, it.skipped, it.score, it.negativeMarks, it.maxScore)
    },
    percentile = percentile,
    rank = rank,
    totalCandidates = totalCandidates,
    passMarks = passMarks,
    questions = questions.map { it.toModel() },
    answers = answers.associate { it.questionId to it.selectedOptionIds.toSet() },
    recommendations = recommendations.map { it.toModel() },
)

fun AttemptHistoryDto.toModel() = MockAttemptHistoryItem(attemptId, mockTitle.toModel(), submittedAt.instant(), score, maxScore, negativeMarks, percentile)

// ---- Planner / dashboard / analytics ----
fun PlanTaskDto.toModel() = PlanTask(
    id, date.date(), type.toEnum(PlanTaskType.LEARN), title.toModel(), topicId, mockId, minutes, reason.toModel(),
    status.toEnum(PlanTaskStatus.PENDING),
)

fun StudyPlanDto.toModel() = StudyPlan(
    id, examId, examName.toModel(), examDate?.date(),
    days.map { d -> StudyDay(d.date.date(), d.tasks.map { it.toModel() }) },
    missedTaskCount,
    adjustments.map { PlanAdjustment(it.id, it.summary.toModel(), it.date.date()) },
)

fun DashboardDto.toModel() = Dashboard(
    greetingName = greetingName,
    primaryTarget = primaryTarget?.toModel(),
    continueLearning = continueLearning?.let { ContinueLearning(it.topicId, it.topicName.toModel(), it.subjectName.toModel(), it.progress) },
    todayTasks = todayTasks.map { it.toModel() },
    dailyTargetMinutes = dailyTargetMinutes,
    studiedTodayMinutes = studiedTodayMinutes,
    streak = Streak(streak.current, streak.longest, streak.activeDays.map { it.date() }),
    syllabusCompletion = syllabusCompletion,
    overallMastery = Mastery(overallMastery.coerceIn(0f, 1f)),
    weakTopics = weakTopics.map { WeakTopic(it.topicId, it.name.toModel(), it.subjectName.toModel(), it.accuracy, it.reason.toModel()) },
    revisionDueCount = revisionDueCount,
    recentMock = recentMock?.toModel(),
    recommendedSets = recommendedSets.map { it.toModel() },
    announcements = announcements.map { it.toModel() },
    weekly = WeeklySummary(weekly.minutesStudied, weekly.questionsAnswered, weekly.accuracy, weekly.accuracyChange, weekly.dailyMinutes),
)

fun AnalyticsDto.toModel() = PerformanceAnalytics(
    overallAccuracy = overallAccuracy,
    questionsAttempted = questionsAttempted,
    avgSecondsPerQuestion = avgSecondsPerQuestion,
    negativeMarksLost = negativeMarksLost,
    syllabusCompletion = syllabusCompletion,
    subjects = subjects.map {
        SubjectPerformance(it.subjectId, it.subjectName.toModel(), it.accuracy, it.attempted, Mastery(it.mastery.coerceIn(0f, 1f)), it.avgSecondsPerQuestion)
    },
    accuracyTrend = accuracyTrend.map { TrendPoint(it.date.date(), it.value) },
    mockScoreTrend = mockScoreTrend.map { TrendPoint(it.date.date(), it.value) },
    speedVsAccuracy = subjects.map { it.subjectName.toModel() to (it.avgSecondsPerQuestion to it.accuracy) },
    repeatedMistakes = repeatedMistakes.map { RepeatedMistake(it.topicId, it.topicName.toModel(), it.pattern.toModel(), it.occurrences) },
    revisionRetention = revisionRetention,
    studyDaysLast30 = studyDaysLast30,
    consistency = consistency,
    recommendations = recommendations.map { it.toModel() },
)

// ---- Revision ----
fun FlashcardDto.toModel() = Flashcard(
    id, deckId, front.toModel(), back.toModel(), origin.toEnum(FlashcardOrigin.USER), topicId,
    SrsState(repetitions, intervalDays, easeFactor, dueAt.instant(), lastReviewedAt?.instant()),
)

fun Flashcard.toDto() = FlashcardDto(
    id, deckId, front.toDto(), back.toDto(), origin.name, topicId,
    srs.repetitions, srs.intervalDays, srs.easeFactor, srs.dueAt.toString(), srs.lastReviewedAt?.toString(),
)

fun TutorMessageDto.toModel() = ChatMessage(
    id = id,
    role = ChatRole.ASSISTANT,
    text = text,
    sources = sources.map { it.toModel() },
    createdAt = createdAt.instant(),
    suggestedFollowUps = suggestedFollowUps.mapNotNull { runCatching { TutorIntent.valueOf(it) }.getOrNull() },
    generatedFlashcards = flashcards.map { it.toModel() },
)

// ---- Account / community ----
fun SubscriptionDto.toModel() = SubscriptionStatus(
    planName?.toModel(), entitlements.mapNotNull { runCatching { Entitlement.valueOf(it) }.getOrNull() }.toSet(),
    expiresAt?.date(), isActive,
)

fun PlanDto.toModel() = SubscriptionPlan(
    id, name.toModel(), priceBdt, periodMonths,
    entitlements.mapNotNull { runCatching { Entitlement.valueOf(it) }.getOrNull() }.toSet(), isPopular,
)

fun AchievementDto.toModel() = Achievement(id, title.toModel(), description.toModel(), unlockedAt?.instant(), progress)
fun TicketDto.toModel() = SupportTicket(id, subject, status.toEnum(TicketStatus.OPEN), createdAt.instant(), lastReply)
fun NotificationDto.toModel() = AppNotification(id, title.toModel(), body.toModel(), createdAt.instant(), isRead)
fun ExportDto.toModel() = DataExportRequest(id, requestedAt.instant(), readyAt?.instant(), downloadUrl)
fun GroupDto.toModel() = StudyGroup(id, name, examShortName, memberCount, isMember, isPrivate, description)
fun DiscussionDto.toModel() = Discussion(id, title, body, authorName, questionId, examShortName, replyCount, upvotes, createdAt.instant(), isResolved)
fun ReplyDto.toModel() = DiscussionReply(id, authorName, body, upvotes, isAccepted, isFromEditor, createdAt.instant())
fun LeaderboardDto.toModel() = Leaderboard(groupId, period, entries.map { LeaderboardEntry(it.rank, it.displayName, it.score, it.isMe) })
