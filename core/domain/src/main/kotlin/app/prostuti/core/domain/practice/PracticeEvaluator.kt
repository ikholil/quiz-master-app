package app.prostuti.core.domain.practice

import app.prostuti.core.domain.exam.NegativeMarking
import app.prostuti.core.model.*
import java.time.Duration

/**
 * Scores a practice session on-device. Used for offline practice and to show instant results;
 * when online the backend result replaces it (and adds server-side recommendations).
 */
object PracticeEvaluator {

    fun evaluate(
        session: PracticeSession,
        answers: List<AnswerRecord>,
        marksPerQuestion: Double = 1.0,
        negativePerWrong: Double = 0.0,
    ): PracticeResult {
        val byId = answers.associateBy { it.questionId }
        var correct = 0
        var incorrect = 0
        var skipped = 0
        val perTopic = linkedMapOf<String, Triple<LocalizedText, Int, Int>>()

        session.questions.forEach { q ->
            val selected = byId[q.id]?.selectedOptionIds.orEmpty()
            val solution = q.solution
            val (c, t) = perTopic[q.topicId]?.let { it.second to it.third } ?: (0 to 0)
            when {
                selected.isEmpty() || solution == null -> skipped++
                NegativeMarking.isAnswerCorrect(selected, solution.correctOptionIds) -> {
                    correct++; perTopic[q.topicId] = Triple(q.topicName, c + 1, t + 1)
                }
                else -> {
                    incorrect++; perTopic[q.topicId] = Triple(q.topicName, c, t + 1)
                }
            }
            if (q.topicId !in perTopic) perTopic[q.topicId] = Triple(q.topicName, 0, 0)
        }
        val score = NegativeMarking.score(correct, incorrect, skipped, marksPerQuestion, negativePerWrong)
        val outcomes = perTopic.map { (id, v) -> TopicOutcome(id, v.first, v.second, v.third) }
        val result = PracticeResult(
            sessionId = session.id,
            mode = session.config.mode,
            correct = correct,
            incorrect = incorrect,
            skipped = skipped,
            score = score.net,
            maxScore = session.questions.size * marksPerQuestion,
            timeSpent = answers.fold(Duration.ZERO) { acc, a -> acc + a.timeSpent },
            topicOutcomes = outcomes,
            questions = session.questions,
            answers = byId,
            nextAction = null,
            isServerVerified = false,
        )
        return result.copy(nextAction = localNextAction(result))
    }

    /** Simple explainable rule used offline; the backend engine replaces it when online. */
    fun localNextAction(result: PracticeResult): Recommendation? {
        val weakest = result.weakTopics.minByOrNull { it.accuracy }
        return when {
            weakest != null -> Recommendation(
                id = "local-weak-${weakest.topicId}",
                title = LocalizedText("Revisit ${weakest.topicName.en}", "${weakest.topicName.bn ?: weakest.topicName.en} আবার পড়ুন"),
                reason = LocalizedText(
                    "You answered ${weakest.correct} of ${weakest.total} correctly in this topic.",
                    "এই টপিকে ${weakest.total}টির মধ্যে ${weakest.correct}টি সঠিক হয়েছে।",
                ),
                action = RecommendedAction.StudyTopic(weakest.topicId),
            )
            result.incorrect > 0 -> Recommendation(
                id = "local-wrong",
                title = LocalizedText("Review your mistakes", "ভুলগুলো আবার দেখুন"),
                reason = LocalizedText(
                    "${result.incorrect} wrong answers were added to your notebook.",
                    "${result.incorrect}টি ভুল উত্তর আপনার নোটবুকে যোগ হয়েছে।",
                ),
                action = RecommendedAction.ReviewWrongAnswers,
            )
            else -> null
        }
    }
}
