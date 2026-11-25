package app.prostuti.core.model

import java.time.Instant

enum class FlashcardOrigin { USER, AI_APPROVED, EDITORIAL }
enum class DeckKind { GENERAL, FORMULA, VOCABULARY, FACTS }

data class FlashcardDeck(
    val id: String,
    val name: LocalizedText,
    val kind: DeckKind,
    val cardCount: Int,
    val dueCount: Int,
    val isAvailableOffline: Boolean,
)

/** Spaced repetition state follows SM-2; see [app.prostuti.core.domain.revision.SpacedRepetition]. */
data class SrsState(
    val repetitions: Int,
    val intervalDays: Int,
    val easeFactor: Double,
    val dueAt: Instant,
    val lastReviewedAt: Instant?,
) {
    companion object {
        fun new(now: Instant) = SrsState(0, 0, 2.5, now, null)
    }
}

data class Flashcard(
    val id: String,
    val deckId: String,
    val front: LocalizedText,
    val back: LocalizedText,
    val origin: FlashcardOrigin,
    val topicId: String?,
    val srs: SrsState,
)

/** Self-rating after revealing a card. */
enum class RecallGrade(val quality: Int) { AGAIN(1), HARD(3), GOOD(4), EASY(5) }

data class Note(
    val id: String,
    val topicId: String?,
    val questionId: String?,
    val title: String,
    val body: String,
    val updatedAt: Instant,
    val isSynced: Boolean,
)

data class RevisionOverview(
    val dueFlashcards: Int,
    val wrongAnswers: Int,
    val savedQuestions: Int,
    val notes: Int,
    val decks: List<FlashcardDeck>,
)
