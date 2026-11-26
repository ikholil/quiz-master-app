package app.prostuti.feature.dashboard

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute

fun NavGraphBuilder.homeGraph(@Suppress("UNUSED_PARAMETER") navController: NavController) {
    composable<HomeRoute> { HomeScreen() }
}
