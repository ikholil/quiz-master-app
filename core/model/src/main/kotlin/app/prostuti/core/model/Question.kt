package app.prostuti.core.model

import java.time.Instant

enum class QuestionType { MCQ, MULTI_SELECT, TRUE_FALSE, SHORT_ANSWER, WRITTEN, VIVA }

/**
 * Where a question comes from. Shown on every question so students know how much to trust it.
 * - OFFICIAL: published by the recruiting authority.
 * - VERIFIED_RECONSTRUCTED: rebuilt from candidate memory, then verified by the editorial team.
 * - COMMUNITY: submitted by students, not yet editorially verified.
 */
enum class QuestionProvenance { OFFICIAL, VERIFIED_RECONSTRUCTED, COMMUNITY }

data class QuestionOption(
    val id: String,
    val text: LocalizedText,
    val imageUrl: String? = null,
)

/** Revealed only after answering in practice, or after submission in a mock exam. */
data class QuestionSolution(
    val correctOptionIds: Set<String>,
    val explanation: LocalizedText,
    /** Why each wrong option is wrong, keyed by option id. */
    val optionExplanations: Map<String, LocalizedText>,
    val modelAnswer: LocalizedText? = null,
)

data class ExamReference(val examName: LocalizedText, val year: Int?, val stage: String?)

data class Passage(val id: String, val title: LocalizedText?, val text: LocalizedText)

data class Question(
    val id: String,
    val type: QuestionType,
    val stem: LocalizedText,
    val options: List<QuestionOption>,
    val passage: Passage?,
    val imageUrl: String?,
    /** Formula shown below the stem, in readable linear notation. */
    val formula: String?,
    val difficulty: Difficulty,
    val provenance: QuestionProvenance,
    val topicId: String,
    val topicName: LocalizedText,
    val subjectName: LocalizedText,
    val examReferences: List<ExamReference>,
    val sources: List<SourceReference>,
    val solution: QuestionSolution?,
    val isBookmarked: Boolean = false,
    val note: String? = null,
)

data class QuestionFilter(
    val query: String = "",
    val examId: String? = null,
    val subjectId: String? = null,
    val topicId: String? = null,
    val difficulty: Difficulty? = null,
    val type: QuestionType? = null,
    val provenance: QuestionProvenance? = null,
    val previousYearOnly: Boolean = false,
    val bookmarkedOnly: Boolean = false,
)

enum class QuestionReportReason { WRONG_ANSWER, TYPO, UNCLEAR, OUTDATED, DUPLICATE, OTHER }

data class QuestionCollection(
    val id: String,
    val name: String,
    val questionCount: Int,
    val updatedAt: Instant,
)

data class WrongAnswerEntry(
    val question: Question,
    val selectedOptionIds: Set<String>,
    val timesWrong: Int,
    val lastWrongAt: Instant,
    val resolved: Boolean,
)
