package app.prostuti.core.model

import java.time.Duration

enum class PracticeMode {
    TOPIC, WEAK_TOPICS, WRONG_ANSWERS, SPEED, ACCURACY, MIXED, PREVIOUS_YEAR, DAILY_CHALLENGE, MARATHON, CUSTOM
}

data class PracticeConfig(
    val mode: PracticeMode,
    val subjectIds: List<String> = emptyList(),
    val topicIds: List<String> = emptyList(),
    val questionCount: Int = 20,
    val difficulty: Difficulty? = null,
    /** Null means untimed. */
    val durationMinutes: Int? = null,
    /** Practice reveals the answer right after each question unless the student opts for exam-style. */
    val instantFeedback: Boolean = true,
    val offlineOnly: Boolean = false,
)

data class PracticeSession(
    val id: String,
    val config: PracticeConfig,
    val questions: List<Question>,
    val isOffline: Boolean,
)

data class AnswerRecord(
    val questionId: String,
    val selectedOptionIds: Set<String>,
    val timeSpent: Duration,
)

data class TopicOutcome(
    val topicId: String,
    val topicName: LocalizedText,
    val correct: Int,
    val total: Int,
) {
    val accuracy: Float get() = if (total == 0) 0f else correct.toFloat() / total
}

data class PracticeResult(
    val sessionId: String,
    val mode: PracticeMode,
    val correct: Int,
    val incorrect: Int,
    val skipped: Int,
    val score: Double,
    val maxScore: Double,
    val timeSpent: Duration,
    val topicOutcomes: List<TopicOutcome>,
    val questions: List<Question>,
    val answers: Map<String, AnswerRecord>,
    val nextAction: Recommendation?,
    /** False when computed on-device while offline; synced to the backend later. */
    val isServerVerified: Boolean,
) {
    val attempted: Int get() = correct + incorrect
    val accuracy: Float get() = if (attempted == 0) 0f else correct.toFloat() / attempted
    val weakTopics: List<TopicOutcome> get() = topicOutcomes.filter { it.total > 0 && it.accuracy < 0.6f }
}

data class PracticeSetSummary(
    val id: String,
    val title: LocalizedText,
    val questionCount: Int,
    val estimatedMinutes: Int,
    val reason: LocalizedText,
    val isDownloaded: Boolean,
)
