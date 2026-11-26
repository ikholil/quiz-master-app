package app.prostuti.feature.learn

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** [examId] null = the student's primary target exam. */
@Serializable data class LearnRoute(val examId: String? = null)
@Serializable data class SubjectRoute(val subjectId: String, val examId: String)
@Serializable data class TopicRoute(val topicId: String)
@Serializable data class MaterialRoute(val materialId: String)

fun NavGraphBuilder.learnGraph(@Suppress("UNUSED_PARAMETER") navController: NavController) {
    composable<LearnRoute> { LearnScreen() }
    composable<SubjectRoute> { SubjectScreen() }
    composable<TopicRoute> { TopicScreen() }
    composable<MaterialRoute> { MaterialScreen() }
}
