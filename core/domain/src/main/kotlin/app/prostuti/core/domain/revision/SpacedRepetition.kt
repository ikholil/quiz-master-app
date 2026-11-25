package app.prostuti.core.domain.revision

import app.prostuti.core.model.RecallGrade
import app.prostuti.core.model.SrsState
import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * SM-2 spaced repetition, run on-device so flashcards work fully offline.
 * Review events are queued and synced; the backend replays them to keep one record per card.
 */
object SpacedRepetition {
    private const val MIN_EASE = 1.3

    fun review(state: SrsState, grade: RecallGrade, now: Instant): SrsState {
        val q = grade.quality
        val ease = max(MIN_EASE, state.easeFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)))
        return if (q < 3) {
            // Lapse: see it again in 10 minutes, restart the ladder.
            SrsState(0, 0, ease, now.plus(Duration.ofMinutes(10)), now)
        } else {
            val reps = state.repetitions + 1
            val interval = when (reps) {
                1 -> 1
                2 -> if (grade == RecallGrade.EASY) 4 else 3
                else -> (state.intervalDays * ease * if (grade == RecallGrade.HARD) 0.8 else 1.0).roundToInt().coerceAtLeast(state.intervalDays + 1)
            }
            SrsState(reps, interval, ease, now.plus(Duration.ofDays(interval.toLong())), now)
        }
    }

    fun isDue(state: SrsState, now: Instant): Boolean = !state.dueAt.isAfter(now)

    /** Preview label for each grade button, e.g. "10m", "3d". */
    fun previewIntervals(state: SrsState, now: Instant): Map<RecallGrade, Duration> =
        RecallGrade.entries.associateWith { Duration.between(now, review(state, it, now).dueAt) }
}
