package app.prostuti.core.domain.exam

import app.prostuti.core.model.Question
import kotlin.random.Random

/**
 * Deterministic shuffling seeded by attempt id: the same attempt always renders the same order,
 * so resuming after a crash or on another device does not reorder questions mid-exam.
 * Option ids are preserved — only display order changes — so answers stay valid server-side.
 */
object OptionShuffler {
    fun shuffle(
        questions: List<Question>,
        seed: String,
        shuffleQuestions: Boolean,
        shuffleOptions: Boolean,
    ): List<Question> {
        val random = Random(seed.hashCode())
        val ordered = if (shuffleQuestions) questions.shuffled(random) else questions
        if (!shuffleOptions) return ordered
        return ordered.map { q ->
            // "All of the above"-style options must stay last; editors flag them with a trailing id suffix.
            val (pinned, free) = q.options.partition { it.id.endsWith(PINNED_SUFFIX) }
            q.copy(options = free.shuffled(Random((seed + q.id).hashCode())) + pinned)
        }
    }

    const val PINNED_SUFFIX = ":pinned"
}
