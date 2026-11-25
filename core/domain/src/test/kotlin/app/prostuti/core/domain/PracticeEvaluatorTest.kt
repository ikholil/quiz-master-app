package app.prostuti.core.domain

import app.prostuti.core.domain.Fixtures.question
import app.prostuti.core.domain.practice.PracticeEvaluator
import app.prostuti.core.model.*
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class PracticeEvaluatorTest {
    private val session = PracticeSession(
        id = "p1",
        config = PracticeConfig(PracticeMode.TOPIC),
        questions = listOf(question("q1", topicId = "a"), question("q2", topicId = "a"), question("q3", topicId = "b"), question("q4", topicId = "b")),
        isOffline = true,
    )

    private fun answer(q: String, opt: String?) =
        AnswerRecord(q, setOfNotNull(opt?.let { "$q-$it" }), Duration.ofSeconds(30))

    @Test fun `counts correct, incorrect and skipped and applies negative marks`() {
        val result = PracticeEvaluator.evaluate(
            session,
            listOf(answer("q1", "a"), answer("q2", "b"), answer("q3", "a"), answer("q4", null)),
            negativePerWrong = 0.25,
        )
        assertEquals(2, result.correct)
        assertEquals(1, result.incorrect)
        assertEquals(1, result.skipped)
        assertEquals(1.75, result.score)
        assertEquals(Duration.ofMinutes(2), result.timeSpent)
        assertFalse(result.isServerVerified)
    }

    @Test fun `weakest topic drives the next action with a reason`() {
        val result = PracticeEvaluator.evaluate(
            session,
            listOf(answer("q1", "b"), answer("q2", "b"), answer("q3", "a"), answer("q4", "a")),
        )
        assertEquals(listOf("a"), result.weakTopics.map { it.topicId })
        val action = assertIs<RecommendedAction.StudyTopic>(result.nextAction?.action)
        assertEquals("a", action.topicId)
        assertEquals("You answered 0 of 2 correctly in this topic.", result.nextAction?.reason?.en)
    }
}
