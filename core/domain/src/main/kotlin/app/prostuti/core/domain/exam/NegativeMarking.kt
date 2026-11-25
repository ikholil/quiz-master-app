package app.prostuti.core.domain.exam

import kotlin.math.round

/**
 * Pure scoring rules shared by practice previews and the mock-result breakdown.
 * The backend remains authoritative for official mock scores; this is used to explain them
 * (e.g. "you lost 7.5 marks to negative marking") and to score offline practice.
 */
object NegativeMarking {

    data class Score(
        val correct: Int,
        val incorrect: Int,
        val skipped: Int,
        val positive: Double,
        val negative: Double,
    ) {
        val net: Double get() = round2(positive - negative)
    }

    fun score(
        correct: Int,
        incorrect: Int,
        skipped: Int,
        marksPerQuestion: Double,
        negativePerWrong: Double,
    ): Score {
        require(correct >= 0 && incorrect >= 0 && skipped >= 0)
        require(marksPerQuestion > 0 && negativePerWrong >= 0)
        return Score(
            correct = correct,
            incorrect = incorrect,
            skipped = skipped,
            positive = round2(correct * marksPerQuestion),
            negative = round2(incorrect * negativePerWrong),
        )
    }

    /**
     * Guessing break-even: answering a question you can narrow to [remainingOptions] choices
     * has positive expected value only if 1/remaining * marks > (1 - 1/remaining) * negative.
     */
    fun isGuessWorthIt(remainingOptions: Int, marksPerQuestion: Double, negativePerWrong: Double): Boolean {
        require(remainingOptions >= 1)
        val p = 1.0 / remainingOptions
        return p * marksPerQuestion - (1 - p) * negativePerWrong > 0
    }

    fun isAnswerCorrect(selected: Set<String>, correct: Set<String>): Boolean =
        selected.isNotEmpty() && selected == correct

    internal fun round2(v: Double): Double = round(v * 100) / 100.0
}
