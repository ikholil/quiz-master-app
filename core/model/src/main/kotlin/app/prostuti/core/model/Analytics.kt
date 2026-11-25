package app.prostuti.core.model

import java.time.LocalDate

data class SubjectPerformance(
    val subjectId: String,
    val subjectName: LocalizedText,
    val accuracy: Float,
    val attempted: Int,
    val mastery: Mastery,
    val avgSecondsPerQuestion: Float,
)

data class TrendPoint(val date: LocalDate, val value: Float)

data class RepeatedMistake(
    val topicId: String,
    val topicName: LocalizedText,
    val pattern: LocalizedText,
    val occurrences: Int,
)

data class PerformanceAnalytics(
    val overallAccuracy: Float,
    val questionsAttempted: Int,
    val avgSecondsPerQuestion: Float,
    val negativeMarksLost: Double,
    val syllabusCompletion: Float,
    val subjects: List<SubjectPerformance>,
    val accuracyTrend: List<TrendPoint>,
    val mockScoreTrend: List<TrendPoint>,
    /** Pairs of (seconds per question, accuracy) per subject for the speed-vs-accuracy view. */
    val speedVsAccuracy: List<Pair<LocalizedText, Pair<Float, Float>>>,
    val repeatedMistakes: List<RepeatedMistake>,
    val revisionRetention: Float,
    val studyDaysLast30: Int,
    val consistency: List<Int>,
    val recommendations: List<Recommendation>,
)

enum class AnalyticsRange { WEEK, MONTH, ALL }
