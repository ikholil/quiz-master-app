package app.prostuti.core.model

import java.time.Instant

enum class AppLanguage(val tag: String) { BENGALI("bn"), ENGLISH("en") }

/**
 * Content delivered in both languages. Bengali is optional because some official
 * English-paper questions only exist in English.
 */
data class LocalizedText(val en: String, val bn: String? = null) {
    fun resolve(language: AppLanguage): String =
        if (language == AppLanguage.BENGALI) bn?.takeIf { it.isNotBlank() } ?: en else en.ifBlank { bn.orEmpty() }

    companion object {
        val Empty = LocalizedText("")
    }
}

/** A citation the UI shows next to official facts, AI answers and question sources. */
data class SourceReference(
    val title: String,
    val publisher: String,
    val url: String?,
    val publishedAt: Instant?,
)

data class Page<T>(val items: List<T>, val nextCursor: String?) {
    val hasMore: Boolean get() = nextCursor != null
}

/**
 * Every recommendation carries a human-readable reason so the student understands
 * why it was suggested (e.g. "Accuracy dropped to 42% in the last 3 sessions").
 */
data class Recommendation(
    val id: String,
    val title: LocalizedText,
    val reason: LocalizedText,
    val action: RecommendedAction,
)

sealed interface RecommendedAction {
    data class PracticeTopic(val topicId: String) : RecommendedAction
    data class StudyTopic(val topicId: String) : RecommendedAction
    data class TakeMock(val mockId: String) : RecommendedAction
    data object ReviewWrongAnswers : RecommendedAction
    data object ReviewFlashcards : RecommendedAction
    data class OpenPracticeSet(val setId: String) : RecommendedAction
}

enum class Difficulty { EASY, MEDIUM, HARD }
