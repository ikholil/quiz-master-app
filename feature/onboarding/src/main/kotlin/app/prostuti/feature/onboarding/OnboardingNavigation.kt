package app.prostuti.feature.onboarding

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** [edit] = true when opened from the profile to change targets and preferences. */
@Serializable data class OnboardingRoute(val edit: Boolean = false)

fun NavGraphBuilder.onboardingScreen(onFinished: () -> Unit, onBack: () -> Unit) {
    composable<OnboardingRoute> { OnboardingScreen(onFinished = onFinished, onExit = onBack) }
}
