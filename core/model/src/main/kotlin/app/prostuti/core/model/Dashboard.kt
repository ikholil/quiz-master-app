package app.prostuti.core.model

import java.time.LocalDate

data class ContinueLearning(
    val topicId: String,
    val topicName: LocalizedText,
    val subjectName: LocalizedText,
    val progress: Float,
)

data class Dashboard(
    val greetingName: String,
    val primaryTarget: TargetExam?,
    val continueLearning: ContinueLearning?,
    val todayTasks: List<PlanTask>,
    val dailyTargetMinutes: Int,
    val studiedTodayMinutes: Int,
    val streak: Streak,
    val syllabusCompletion: Float,
    val overallMastery: Mastery,
    val weakTopics: List<WeakTopic>,
    val revisionDueCount: Int,
    val recentMock: MockAttemptHistoryItem?,
    val recommendedSets: List<PracticeSetSummary>,
    val announcements: List<Announcement>,
    val weekly: WeeklySummary,
)

data class Streak(val current: Int, val longest: Int, val activeDays: List<LocalDate>)

data class WeakTopic(
    val topicId: String,
    val name: LocalizedText,
    val subjectName: LocalizedText,
    val accuracy: Float,
    val reason: LocalizedText,
)

data class WeeklySummary(
    val minutesStudied: Int,
    val questionsAnswered: Int,
    val accuracy: Float,
    val accuracyChange: Float,
    val dailyMinutes: List<Int>,
)
