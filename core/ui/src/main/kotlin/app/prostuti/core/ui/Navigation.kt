package app.prostuti.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import app.prostuti.core.model.PracticeMode
import app.prostuti.core.model.TutorIntent

/**
 * Cross-feature navigation contract. Features never import each other: they ask for a
 * [Destination] and the app module maps it to a concrete route.
 */
sealed interface Destination {
    data object Back : Destination
    data object Home : Destination
    data object ExamDirectory : Destination
    data class Exam(val examId: String) : Destination
    data class Syllabus(val examId: String?) : Destination
    data class Subject(val subjectId: String, val examId: String) : Destination
    data class Topic(val topicId: String) : Destination
    data class Material(val materialId: String) : Destination
    data class QuestionBank(val examId: String? = null, val topicId: String? = null) : Destination
    data class Question(val questionId: String) : Destination
    data object WrongAnswers : Destination
    data object PracticeHub : Destination
    data class Practice(val mode: PracticeMode, val topicId: String? = null, val setId: String? = null) : Destination
    data class MockList(val examId: String? = null) : Destination
    data class MockExam(val mockId: String) : Destination
    data class MockResult(val attemptId: String) : Destination
    data object MockHistory : Destination
    data object Analytics : Destination
    data class Assistant(val questionId: String? = null, val topicId: String? = null, val intent: TutorIntent? = null) : Destination
    data object Planner : Destination
    data object Revision : Destination
    data class Flashcards(val deckId: String? = null, val quick: Boolean = false) : Destination
    data object Notes : Destination
    data class NoteEditor(val noteId: String? = null, val topicId: String? = null) : Destination
    data object Downloads : Destination
    data object Community : Destination
    data class NewDiscussion(val questionId: String? = null) : Destination
    data object Subscription : Destination
    data object EditPreferences : Destination
    data object Notifications : Destination
    data object Settings : Destination
}

fun interface AppNavigator {
    fun navigate(destination: Destination)
}

val LocalNavigator = staticCompositionLocalOf<AppNavigator> { AppNavigator { } }

fun app.prostuti.core.model.PlanTask.destination(): Destination = when (type) {
    app.prostuti.core.model.PlanTaskType.LEARN -> topicId?.let { Destination.Topic(it) } ?: Destination.Syllabus(null)
    app.prostuti.core.model.PlanTaskType.PRACTICE -> Destination.Practice(PracticeMode.TOPIC, topicId)
    app.prostuti.core.model.PlanTaskType.REVISE -> Destination.Revision
    app.prostuti.core.model.PlanTaskType.FLASHCARDS -> Destination.Flashcards()
    app.prostuti.core.model.PlanTaskType.MOCK -> mockId?.let { Destination.MockExam(it) } ?: Destination.MockList()
}

fun app.prostuti.core.model.RecommendedAction.destination(): Destination = when (this) {
    is app.prostuti.core.model.RecommendedAction.PracticeTopic -> Destination.Practice(PracticeMode.TOPIC, topicId)
    is app.prostuti.core.model.RecommendedAction.StudyTopic -> Destination.Topic(topicId)
    is app.prostuti.core.model.RecommendedAction.TakeMock -> Destination.MockExam(mockId)
    app.prostuti.core.model.RecommendedAction.ReviewWrongAnswers -> Destination.WrongAnswers
    app.prostuti.core.model.RecommendedAction.ReviewFlashcards -> Destination.Flashcards()
    is app.prostuti.core.model.RecommendedAction.OpenPracticeSet -> Destination.Practice(PracticeMode.CUSTOM, setId = setId)
}
