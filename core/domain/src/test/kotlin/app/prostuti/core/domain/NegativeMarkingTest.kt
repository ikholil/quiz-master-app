package app.prostuti.core.domain

import app.prostuti.core.domain.exam.NegativeMarking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NegativeMarkingTest {
    @Test fun `BCS preliminary style scoring deducts half a mark per wrong answer`() {
        val s = NegativeMarking.score(correct = 150, incorrect = 30, skipped = 20, marksPerQuestion = 1.0, negativePerWrong = 0.5)
        assertEquals(150.0, s.positive)
        assertEquals(15.0, s.negative)
        assertEquals(135.0, s.net)
    }

    @Test fun `quarter negative marking keeps two decimals`() {
        val s = NegativeMarking.score(10, 3, 0, 1.0, 0.25)
        assertEquals(0.75, s.negative)
        assertEquals(9.25, s.net)
    }

    @Test fun `net score can go below zero`() {
        assertEquals(-1.0, NegativeMarking.score(0, 2, 0, 1.0, 0.5).net)
    }

    @Test fun `multi select must match exactly`() {
        assertTrue(NegativeMarking.isAnswerCorrect(setOf("a", "b"), setOf("b", "a")))
        assertFalse(NegativeMarking.isAnswerCorrect(setOf("a"), setOf("a", "b")))
        assertFalse(NegativeMarking.isAnswerCorrect(emptySet(), emptySet()))
    }

    @Test fun `guessing between two options is worth it at half negative`() {
        assertTrue(NegativeMarking.isGuessWorthIt(2, 1.0, 0.5))
        // Four options at 0.5 negative: 0.25 - 0.375 < 0
        assertFalse(NegativeMarking.isGuessWorthIt(4, 1.0, 0.5))
    }
}
