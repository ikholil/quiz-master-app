package app.prostuti.core.domain

import app.prostuti.core.domain.revision.SpacedRepetition
import app.prostuti.core.model.RecallGrade
import app.prostuti.core.model.SrsState
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpacedRepetitionTest {
    private val now = Instant.parse("2026-11-25T08:00:00Z")

    @Test fun `good answers climb the interval ladder`() {
        var s = SrsState.new(now)
        s = SpacedRepetition.review(s, RecallGrade.GOOD, now)
        assertEquals(1, s.intervalDays)
        s = SpacedRepetition.review(s, RecallGrade.GOOD, now)
        assertEquals(3, s.intervalDays)
        s = SpacedRepetition.review(s, RecallGrade.GOOD, now)
        assertTrue(s.intervalDays >= 7, "third interval was ${s.intervalDays}")
    }

    @Test fun `again resets repetitions and shows the card in ten minutes`() {
        val learned = SrsState(4, 20, 2.5, now, now)
        val lapsed = SpacedRepetition.review(learned, RecallGrade.AGAIN, now)
        assertEquals(0, lapsed.repetitions)
        assertEquals(now.plus(Duration.ofMinutes(10)), lapsed.dueAt)
        assertTrue(lapsed.easeFactor < 2.5)
    }

    @Test fun `ease never drops below the floor`() {
        var s = SrsState.new(now)
        repeat(20) { s = SpacedRepetition.review(s, RecallGrade.AGAIN, now) }
        assertTrue(s.easeFactor >= 1.3)
    }

    @Test fun `due check uses due date inclusive`() {
        assertTrue(SpacedRepetition.isDue(SrsState.new(now), now))
    }
}
