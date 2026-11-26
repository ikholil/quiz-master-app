package app.prostuti.feature.questionbank

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data class QuestionBankRoute(val examId: String? = null, val topicId: String? = null)
@Serializable data class QuestionDetailRoute(val questionId: String)
@Serializable data object WrongAnswersRoute

fun NavGraphBuilder.questionBankGraph(@Suppress("UNUSED_PARAMETER") navController: NavController) {
    composable<QuestionBankRoute> { QuestionBankScreen() }
    composable<QuestionDetailRoute> { QuestionDetailScreen() }
    composable<WrongAnswersRoute> { WrongAnswersScreen() }
}
