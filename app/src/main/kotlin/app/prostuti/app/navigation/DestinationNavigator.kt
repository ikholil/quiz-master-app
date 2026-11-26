package app.prostuti.app.navigation

import androidx.navigation.NavHostController
import app.prostuti.core.ui.AppNavigator
import app.prostuti.core.ui.Destination
import app.prostuti.feature.analytics.AnalyticsRoute
import app.prostuti.feature.assistant.AssistantRoute
import app.prostuti.feature.community.CommunityRoute
import app.prostuti.feature.community.NewDiscussionRoute
import app.prostuti.feature.dashboard.HomeRoute
import app.prostuti.feature.exams.ExamDetailRoute
import app.prostuti.feature.exams.ExamDirectoryRoute
import app.prostuti.feature.learn.LearnRoute
import app.prostuti.feature.learn.MaterialRoute
import app.prostuti.feature.learn.SubjectRoute
import app.prostuti.feature.learn.TopicRoute
import app.prostuti.feature.mockexam.MockExamRoute
import app.prostuti.feature.mockexam.MockHistoryRoute
import app.prostuti.feature.mockexam.MockListRoute
import app.prostuti.feature.mockexam.MockResultRoute
import app.prostuti.feature.offline.DownloadsRoute
import app.prostuti.feature.onboarding.OnboardingRoute
import app.prostuti.feature.planner.PlannerRoute
import app.prostuti.feature.practice.PracticeHubRoute
import app.prostuti.feature.practice.PracticeSessionRoute
import app.prostuti.feature.profile.NotificationsRoute
import app.prostuti.feature.profile.SettingsRoute
import app.prostuti.feature.profile.SubscriptionRoute
import app.prostuti.feature.questionbank.QuestionBankRoute
import app.prostuti.feature.questionbank.QuestionDetailRoute
import app.prostuti.feature.questionbank.WrongAnswersRoute
import app.prostuti.feature.revision.FlashcardsRoute
import app.prostuti.feature.revision.NoteEditorRoute
import app.prostuti.feature.revision.NotesRoute
import app.prostuti.feature.revision.RevisionRoute

/** The only place that knows every feature's routes. */
class DestinationNavigator(private val nav: NavHostController) : AppNavigator {
    override fun navigate(destination: Destination) {
        val route: Any = when (destination) {
            Destination.Back -> { nav.popBackStack(); return }
            Destination.Home -> HomeRoute
            Destination.ExamDirectory -> ExamDirectoryRoute
            is Destination.Exam -> ExamDetailRoute(destination.examId)
            is Destination.Syllabus -> LearnRoute(destination.examId)
            is Destination.Subject -> SubjectRoute(destination.subjectId, destination.examId)
            is Destination.Topic -> TopicRoute(destination.topicId)
            is Destination.Material -> MaterialRoute(destination.materialId)
            is Destination.QuestionBank -> QuestionBankRoute(destination.examId, destination.topicId)
            is Destination.Question -> QuestionDetailRoute(destination.questionId)
            Destination.WrongAnswers -> WrongAnswersRoute
            Destination.PracticeHub -> PracticeHubRoute
            is Destination.Practice -> PracticeSessionRoute(destination.mode.name, destination.topicId, destination.setId)
            is Destination.MockList -> MockListRoute(destination.examId)
            is Destination.MockExam -> MockExamRoute(destination.mockId)
            is Destination.MockResult -> {
                // Leaving an exam for its result must not allow "back" into the submitted exam.
                nav.navigate(MockResultRoute(destination.attemptId)) { popUpTo<MockExamRoute> { inclusive = true } }
                return
            }
            Destination.MockHistory -> MockHistoryRoute
            Destination.Analytics -> AnalyticsRoute
            is Destination.Assistant -> AssistantRoute(destination.questionId, destination.topicId, destination.intent?.name)
            Destination.Planner -> PlannerRoute
            Destination.Revision -> RevisionRoute
            is Destination.Flashcards -> FlashcardsRoute(destination.deckId, destination.quick)
            Destination.Notes -> NotesRoute
            is Destination.NoteEditor -> NoteEditorRoute(destination.noteId, destination.topicId)
            Destination.Downloads -> DownloadsRoute
            Destination.Community -> CommunityRoute
            is Destination.NewDiscussion -> NewDiscussionRoute(destination.questionId)
            Destination.Subscription -> SubscriptionRoute
            Destination.EditPreferences -> OnboardingRoute(edit = true)
            Destination.Notifications -> NotificationsRoute
            Destination.Settings -> SettingsRoute
        }
        nav.navigate(route) { launchSingleTop = true }
    }
}
