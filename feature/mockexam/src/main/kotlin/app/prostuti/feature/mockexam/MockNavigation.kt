package app.prostuti.feature.mockexam

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data class MockListRoute(val examId: String? = null)
@Serializable internal data class MockInstructionsRoute(val mockId: String)
@Serializable data class MockExamRoute(val mockId: String)
@Serializable data class MockResultRoute(val attemptId: String)
@Serializable data object MockHistoryRoute

fun NavGraphBuilder.mockExamGraph(navController: NavController) {
    composable<MockListRoute> { MockListScreen(onOpen = { navController.navigate(MockInstructionsRoute(it)) }) }
    composable<MockInstructionsRoute> {
        MockInstructionsScreen(onStart = { id ->
            navController.navigate(MockExamRoute(id)) { popUpTo<MockInstructionsRoute> { inclusive = true } }
        })
    }
    composable<MockExamRoute> { MockExamScreen() }
    composable<MockResultRoute> { MockResultScreen() }
    composable<MockHistoryRoute> { MockHistoryScreen() }
}
