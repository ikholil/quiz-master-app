package app.prostuti.core.domain

import app.prostuti.core.domain.Fixtures.question
import app.prostuti.core.domain.Fixtures.section
import app.prostuti.core.domain.exam.ExamSessionState
import app.prostuti.core.domain.exam.QuestionStatus
import app.prostuti.core.model.QuestionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExamSessionStateTest {
    private fun state(switching: Boolean = true) = ExamSessionState(
        sections = listOf(
            section("s1", question("q1"), question("q2")),
            section("s2", question("q3"), question("q4", type = QuestionType.MULTI_SELECT)),
        ),
        allowSectionSwitching = switching,
    ).visitCurrent()

    @Test fun `next and previous cross section boundaries`() {
        var s = state()
        s = s.next().next()
        assertEquals("q3", s.currentQuestion.id)
        assertEquals(1, s.sectionIndex)
        s = s.previous()
        assertEquals("q2", s.currentQuestion.id)
        assertTrue(state().previous().isFirst)
    }

    @Test fun `single choice replaces previous selection and tapping again clears it`() {
        var s = state().select("q1-a").select("q1-b")
        assertEquals(setOf("q1-b"), s.answers["q1"])
        s = s.select("q1-b")
        assertEquals(emptySet(), s.answers["q1"])
        assertEquals(0, s.answeredCount)
    }

    @Test fun `multi select toggles options`() {
        var s = state().goTo("q4")
        s = s.select("q4-a").select("q4-c")
        assertEquals(setOf("q4-a", "q4-c"), s.answers["q4"])
        s = s.select("q4-a")
        assertEquals(setOf("q4-c"), s.answers["q4"])
    }

    @Test fun `answer sheet statuses reflect visits, answers and review marks`() {
        var s = state()
        assertEquals(QuestionStatus.NOT_ANSWERED, s.statusOf("q1"))
        assertEquals(QuestionStatus.NOT_VISITED, s.statusOf("q2"))
        s = s.toggleMarkForReview()
        assertEquals(QuestionStatus.MARKED_FOR_REVIEW, s.statusOf("q1"))
        s = s.select("q1-a")
        assertEquals(QuestionStatus.ANSWERED_AND_MARKED, s.statusOf("q1"))
        s = s.toggleMarkForReview()
        assertEquals(QuestionStatus.ANSWERED, s.statusOf("q1"))
        assertEquals(0, s.markedCount)
    }

    @Test fun `locked sections cannot be revisited`() {
        var s = state(switching = false).next().next()
        assertEquals(1, s.sectionIndex)
        assertEquals(s, s.goToSection(0))
        assertEquals("q3", s.goTo("q1").currentQuestion.id)
    }

    @Test fun `changes mark the state dirty until saved`() {
        val s = state().select("q1-a")
        assertTrue(s.dirty)
        assertFalse(s.markSaved().dirty)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `selecting an option from another question is rejected`() {
        state().select("q2-a")
    }
}
