package app.prostuti.app

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.setContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.prostuti.app.navigation.ProstutiAppRoot
import app.prostuti.core.designsystem.theme.ProstutiTheme
import app.prostuti.core.domain.repository.ThemePreference
import app.prostuti.core.model.AppLanguage
import app.prostuti.core.ui.BuildInfo
import app.prostuti.core.ui.LocalAppLanguage
import app.prostuti.core.ui.LocalBuildInfo
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep the splash until we know whether the student is signed in (usually < 100 ms).
        splash.setKeepOnScreenCondition { viewModel.session.value.phase() == AppPhase.LOADING || viewModel.settings.value == null }

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val session by viewModel.session.collectAsStateWithLifecycle()
            val online by viewModel.isOnline.collectAsStateWithLifecycle()
            val dark = when (settings?.theme) {
                ThemePreference.DARK -> true
                ThemePreference.LIGHT -> false
                else -> isSystemInDarkTheme()
            }
            enableEdgeToEdge(
                statusBarStyle = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            )
            CompositionLocalProvider(
                LocalAppLanguage provides (settings?.language ?: AppLanguage.BENGALI),
                LocalBuildInfo provides BuildInfo(BuildConfig.MOCK_BACKEND, BuildConfig.VERSION_NAME),
            ) {
                ProstutiTheme(darkTheme = dark) {
                    ProstutiAppRoot(phase = session.phase(), isOnline = online)
                }
            }
        }
    }
}

@Suppress("unused") private val ctx = LocalContext
