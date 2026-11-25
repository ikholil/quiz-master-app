package app.prostuti.core.domain

import app.prostuti.core.domain.Fixtures.question
import app.prostuti.core.domain.exam.OptionShuffler
import app.prostuti.core.model.QuestionOption
import app.prostuti.core.model.LocalizedText
import kotlin.test.Test
import kotlin.test.assertEquals

class OptionShufflerTest {
    private val questions = (1..10).map { question("q$it") }

    @Test fun `same seed gives the same order so resumed attempts are stable`() {
        val a = OptionShuffler.shuffle(questions, "attempt-1", true, true)
        val b = OptionShuffler.shuffle(questions, "attempt-1", true, true)
        assertEquals(a.map { it.id }, b.map { it.id })
        assertEquals(a.map { q -> q.options.map { it.id } }, b.map { q -> q.options.map { it.id } })
    }

    @Test fun `shuffling keeps every option id`() {
        val shuffled = OptionShuffler.shuffle(questions, "x", true, true)
        assertEquals(questions.map { it.id }.toSet(), shuffled.map { it.id }.toSet())
        shuffled.forEach { q -> assertEquals(setOf("${q.id}-a", "${q.id}-b", "${q.id}-c", "${q.id}-d"), q.options.map { it.id }.toSet()) }
    }

    @Test fun `pinned options stay last`() {
        val q = question("p").let { it.copy(options = it.options + QuestionOption("p-all${OptionShuffler.PINNED_SUFFIX}", LocalizedText("All of the above"))) }
        repeat(5) { i ->
            val out = OptionShuffler.shuffle(listOf(q), "seed$i", false, true).single()
            assertEquals("p-all${OptionShuffler.PINNED_SUFFIX}", out.options.last().id)
        }
    }
}
