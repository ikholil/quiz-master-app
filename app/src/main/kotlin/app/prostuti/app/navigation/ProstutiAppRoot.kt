package app.prostuti.app.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.prostuti.app.AppPhase
import app.prostuti.app.R
import app.prostuti.core.designsystem.component.OfflineBanner
import app.prostuti.core.ui.LocalBuildInfo
import app.prostuti.core.ui.LocalNavigator
import app.prostuti.feature.analytics.analyticsGraph
import app.prostuti.feature.assistant.assistantGraph
import app.prostuti.feature.auth.AuthGraph
import app.prostuti.feature.auth.authGraph
import app.prostuti.feature.community.communityGraph
import app.prostuti.feature.dashboard.HomeRoute
import app.prostuti.feature.dashboard.homeGraph
import app.prostuti.feature.exams.examsGraph
import app.prostuti.feature.learn.LearnRoute
import app.prostuti.feature.learn.learnGraph
import app.prostuti.feature.mockexam.mockExamGraph
import app.prostuti.feature.offline.offlineGraph
import app.prostuti.feature.onboarding.OnboardingRoute
import app.prostuti.feature.onboarding.onboardingScreen
import app.prostuti.feature.planner.plannerGraph
import app.prostuti.feature.practice.PracticeHubRoute
import app.prostuti.feature.practice.practiceGraph
import app.prostuti.feature.profile.ProfileRoute
import app.prostuti.feature.profile.profileGraph
import app.prostuti.feature.questionbank.questionBankGraph
import app.prostuti.feature.revision.RevisionRoute
import app.prostuti.feature.revision.revisionGraph

@Composable
fun ProstutiAppRoot(phase: AppPhase, isOnline: Boolean) {
    if (phase == AppPhase.LOADING) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    // A new NavHost per phase: signing out or finishing onboarding clears the whole back stack.
    key(phase) {
        val navController = rememberNavController()
        val navigator = remember(navController) { DestinationNavigator(navController) }
        CompositionLocalProvider(LocalNavigator provides navigator) {
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                if (LocalBuildInfo.current.isMockBackend) DevBanner()
                OfflineBanner(visible = !isOnline && phase == AppPhase.MAIN, message = stringResource(app.prostuti.core.ui.R.string.ui_offline_banner), modifier = if (LocalBuildInfo.current.isMockBackend) Modifier else Modifier.statusBarsPadding())
                Box(Modifier.weight(1f)) {
                    androidx.compose.material3.Scaffold(
                        bottomBar = { if (phase == AppPhase.MAIN) BottomBar(navController) },
                        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                        containerColor = MaterialTheme.colorScheme.background,
                    ) { padding ->
                        NavHost(
                            navController = navController,
                            startDestination = when (phase) {
                                AppPhase.SIGNED_OUT -> AuthGraph
                                AppPhase.ONBOARDING -> OnboardingRoute()
                                else -> HomeRoute
                            },
                            modifier = Modifier.padding(padding),
                            enterTransition = { fadeIn() },
                            exitTransition = { fadeOut() },
                        ) {
                            authGraph(navController)
                            onboardingScreen(onFinished = { navController.popBackStack() }, onBack = { navController.popBackStack() })
                            homeGraph(navController)
                            examsGraph(navController)
                            learnGraph(navController)
                            questionBankGraph(navController)
                            practiceGraph(navController)
                            mockExamGraph(navController)
                            analyticsGraph(navController)
                            assistantGraph(navController)
                            plannerGraph(navController)
                            revisionGraph(navController)
                            offlineGraph(navController)
                            communityGraph(navController)
                            profileGraph(navController)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DevBanner() {
    Text(
        stringResource(R.string.dev_banner),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.tertiaryContainer).statusBarsPadding().padding(vertical = 2.dp),
    )
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val visible = TopLevelTab.entries.any { tab -> destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true }
    AnimatedVisibility(visible, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp) {
            TopLevelTab.entries.forEach { tab ->
                val selected = destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(if (selected) tab.selectedIcon else tab.icon, null) },
                    label = { Text(stringResource(tab.label), maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                )
            }
        }
    }
}

enum class TopLevelTab(val route: Any, val label: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector, val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME(HomeRoute, R.string.nav_home, Icons.Outlined.Home, Icons.Rounded.Home),
    LEARN(LearnRoute(), R.string.nav_learn, Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Rounded.MenuBook),
    PRACTICE(PracticeHubRoute, R.string.nav_practice, Icons.Outlined.EditNote, Icons.Rounded.EditNote),
    REVISE(RevisionRoute, R.string.nav_revise, Icons.Outlined.Style, Icons.Rounded.Style),
    ME(ProfileRoute, R.string.nav_me, Icons.Outlined.Person, Icons.Rounded.Person),
}

@Suppress("unused") private val a = Alignment.Center
