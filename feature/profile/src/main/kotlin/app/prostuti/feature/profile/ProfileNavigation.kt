package app.prostuti.feature.profile

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object ProfileRoute
@Serializable data object SettingsRoute
@Serializable data object SubscriptionRoute
@Serializable data object NotificationsRoute
@Serializable internal data object PrivacyRoute
@Serializable internal data object SupportRoute
@Serializable internal data object AccountDataRoute

fun NavGraphBuilder.profileGraph(navController: NavController) {
    composable<ProfileRoute> {
        ProfileScreen(
            onPrivacy = { navController.navigate(PrivacyRoute) },
            onSupport = { navController.navigate(SupportRoute) },
            onAccountData = { navController.navigate(AccountDataRoute) },
        )
    }
    composable<SettingsRoute> { SettingsScreen() }
    composable<SubscriptionRoute> { SubscriptionScreen() }
    composable<NotificationsRoute> { NotificationsScreen() }
    composable<PrivacyRoute> { PrivacyScreen() }
    composable<SupportRoute> { SupportScreen() }
    composable<AccountDataRoute> { AccountDataScreen() }
}
