package app.prostuti.core.domain

import app.prostuti.core.model.*

object Fixtures {
    fun question(
        id: String,
        type: QuestionType = QuestionType.MCQ,
        correct: Set<String> = setOf("$id-a"),
        topicId: String = "t1",
    ) = Question(
        id = id,
        type = type,
        stem = LocalizedText("Question $id"),
        options = listOf("a", "b", "c", "d").map { QuestionOption("$id-$it", LocalizedText(it.uppercase())) },
        passage = null,
        imageUrl = null,
        formula = null,
        difficulty = Difficulty.MEDIUM,
        provenance = QuestionProvenance.OFFICIAL,
        topicId = topicId,
        topicName = LocalizedText("Topic $topicId"),
        subjectName = LocalizedText("Subject"),
        examReferences = emptyList(),
        sources = emptyList(),
        solution = QuestionSolution(correct, LocalizedText("Because."), emptyMap()),
    )

    fun section(id: String, vararg questions: Question, negative: Double = 0.5) = AttemptSection(
        MockSection(id, LocalizedText("Section $id"), questions.size, 1.0, negative, null),
        questions.toList(),
    )
}
