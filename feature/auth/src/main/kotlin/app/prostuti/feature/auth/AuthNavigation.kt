package app.prostuti.feature.auth

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable

@Serializable data object AuthGraph
@Serializable internal data object WelcomeRoute
@Serializable internal data object PhoneRoute
@Serializable internal data class OtpRoute(val challengeId: String, val phone: String, val resendAtEpochMs: Long, val codeLength: Int)
@Serializable internal data object EmailRoute

/** Sign-in flow. On success the session flow changes and the app root leaves this graph. */
fun NavGraphBuilder.authGraph(navController: NavController) {
    navigation<AuthGraph>(startDestination = WelcomeRoute) {
        composable<WelcomeRoute> {
            WelcomeScreen(
                onPhone = { navController.navigate(PhoneRoute) },
                onEmail = { navController.navigate(EmailRoute) },
            )
        }
        composable<PhoneRoute> {
            PhoneScreen(
                onBack = navController::popBackStack,
                onCodeSent = { c -> navController.navigate(OtpRoute(c.challengeId, c.phone, c.resendAvailableAt.toEpochMilli(), c.codeLength)) },
            )
        }
        composable<OtpRoute> { OtpScreen(onBack = navController::popBackStack) }
        composable<EmailRoute> { EmailScreen(onBack = navController::popBackStack) }
    }
}
