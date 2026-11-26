package app.prostuti.feature.exams

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object ExamDirectoryRoute
@Serializable data class ExamDetailRoute(val examId: String)
@Serializable internal data class CompareRoute(val examIds: List<String>)

fun NavGraphBuilder.examsGraph(navController: NavController) {
    composable<ExamDirectoryRoute> { ExamDirectoryScreen(onCompare = { navController.navigate(CompareRoute(it)) }) }
    composable<ExamDetailRoute> { ExamDetailScreen() }
    composable<CompareRoute> { CompareScreen() }
}
