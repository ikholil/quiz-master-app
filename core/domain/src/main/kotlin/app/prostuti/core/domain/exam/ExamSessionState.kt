package app.prostuti.core.domain.exam

import app.prostuti.core.model.AttemptSection
import app.prostuti.core.model.Question
import app.prostuti.core.model.QuestionType

enum class QuestionStatus { NOT_VISITED, NOT_ANSWERED, ANSWERED, MARKED_FOR_REVIEW, ANSWERED_AND_MARKED }

/**
 * Immutable state of an exam-style session (used by mock exams and exam-style practice).
 * All transitions are pure functions, so navigation and answer rules are unit-tested.
 */
data class ExamSessionState(
    val sections: List<AttemptSection>,
    val sectionIndex: Int = 0,
    val questionIndex: Int = 0,
    val answers: Map<String, Set<String>> = emptyMap(),
    val markedForReview: Set<String> = emptySet(),
    val visited: Set<String> = emptySet(),
    val allowSectionSwitching: Boolean = true,
    /** Answers changed since the last successful autosave. */
    val dirty: Boolean = false,
) {
    init {
        require(sections.isNotEmpty() && sections.all { it.questions.isNotEmpty() }) { "Exam needs questions" }
    }

    val currentSection: AttemptSection get() = sections[sectionIndex]
    val currentQuestion: Question get() = currentSection.questions[questionIndex]
    val allQuestions: List<Question> get() = sections.flatMap { it.questions }
    val totalQuestions: Int get() = allQuestions.size
    val answeredCount: Int get() = allQuestions.count { answers[it.id].orEmpty().isNotEmpty() }
    val markedCount: Int get() = markedForReview.size
    val unansweredCount: Int get() = totalQuestions - answeredCount
    val globalIndex: Int get() = sections.take(sectionIndex).sumOf { it.questions.size } + questionIndex
    val isFirst: Boolean get() = globalIndex == 0
    val isLast: Boolean get() = globalIndex == totalQuestions - 1

    fun statusOf(questionId: String): QuestionStatus {
        val answered = answers[questionId].orEmpty().isNotEmpty()
        val marked = questionId in markedForReview
        return when {
            answered && marked -> QuestionStatus.ANSWERED_AND_MARKED
            marked -> QuestionStatus.MARKED_FOR_REVIEW
            answered -> QuestionStatus.ANSWERED
            questionId in visited -> QuestionStatus.NOT_ANSWERED
            else -> QuestionStatus.NOT_VISITED
        }
    }

    fun visitCurrent(): ExamSessionState = copy(visited = visited + currentQuestion.id)

    /** Single-answer types replace the selection; multi-select toggles. Tapping a selected single answer clears it. */
    fun select(optionId: String): ExamSessionState {
        val q = currentQuestion
        require(q.options.any { it.id == optionId }) { "Unknown option $optionId" }
        val current = answers[q.id].orEmpty()
        val next = when (q.type) {
            QuestionType.MULTI_SELECT -> if (optionId in current) current - optionId else current + optionId
            else -> if (current == setOf(optionId)) emptySet() else setOf(optionId)
        }
        return copy(answers = answers + (q.id to next), dirty = true).visitCurrent()
    }

    fun clearAnswer(): ExamSessionState =
        copy(answers = answers - currentQuestion.id, dirty = true)

    fun toggleMarkForReview(): ExamSessionState {
        val id = currentQuestion.id
        return copy(
            markedForReview = if (id in markedForReview) markedForReview - id else markedForReview + id,
            dirty = true,
        ).visitCurrent()
    }

    fun next(): ExamSessionState = goToGlobal(globalIndex + 1)
    fun previous(): ExamSessionState = goToGlobal(globalIndex - 1)

    fun goToSection(index: Int): ExamSessionState {
        if (index == sectionIndex || index !in sections.indices) return this
        if (!allowSectionSwitching) return this
        return copy(sectionIndex = index, questionIndex = 0).visitCurrent()
    }

    fun goTo(questionId: String): ExamSessionState {
        val target = allQuestions.indexOfFirst { it.id == questionId }
        return if (target < 0) this else goToGlobal(target)
    }

    private fun goToGlobal(target: Int): ExamSessionState {
        if (target !in 0 until totalQuestions) return this
        var remaining = target
        sections.forEachIndexed { s, section ->
            if (remaining < section.questions.size) {
                if (s != sectionIndex && !allowSectionSwitching && s < sectionIndex) return this
                return copy(sectionIndex = s, questionIndex = remaining).visitCurrent()
            }
            remaining -= section.questions.size
        }
        return this
    }

    fun markSaved(): ExamSessionState = copy(dirty = false)
}
