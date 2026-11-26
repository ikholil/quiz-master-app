package app.prostuti.feature.practice

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object PracticeHubRoute

/** Full practice configuration as route arguments so a session survives process death. */
@Serializable
data class PracticeSessionRoute(
    val mode: String,
    val topicId: String? = null,
    val setId: String? = null,
    val subjectIds: List<String> = emptyList(),
    val count: Int = 0,
    val difficulty: String? = null,
    val minutes: Int = -1,
    val instantFeedback: Boolean = true,
    val offlineOnly: Boolean = false,
)

fun NavGraphBuilder.practiceGraph(navController: NavController) {
    composable<PracticeHubRoute> { PracticeHubScreen(onStart = { navController.navigate(it) }) }
    composable<PracticeSessionRoute> { PracticeSessionScreen() }
}
