package app.prostuti.core.model

import java.time.Instant

enum class ChatRole { STUDENT, ASSISTANT }

/** What the student wants from the tutor; the backend uses it to pick a prompt and retrieval scope. */
enum class TutorIntent {
    EXPLAIN_SIMPLE_BANGLA, EXPLAIN_ENGLISH, SOLVE_STEP_BY_STEP, WHY_CORRECT, WHY_OTHERS_WRONG,
    HINT, SUMMARIZE, REVISION_NOTES, FLASHCARDS, PRACTICE_QUESTIONS, EXPLAIN_MISTAKES, FREE_CHAT
}

data class TutorContext(
    val questionId: String? = null,
    val topicId: String? = null,
    val materialId: String? = null,
)

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val text: String,
    val sources: List<SourceReference>,
    val createdAt: Instant,
    val suggestedFollowUps: List<TutorIntent> = emptyList(),
    val generatedFlashcards: List<Flashcard> = emptyList(),
)

data class TutorConversation(
    val id: String,
    val title: String,
    val context: TutorContext,
    val messages: List<ChatMessage>,
)

data class AiUsage(val usedToday: Int, val dailyLimit: Int) {
    val remaining: Int get() = (dailyLimit - usedToday).coerceAtLeast(0)
}
