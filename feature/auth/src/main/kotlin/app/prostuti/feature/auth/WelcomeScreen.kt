package app.prostuti.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.prostuti.core.designsystem.component.IconBadge
import app.prostuti.core.designsystem.component.PtButton
import app.prostuti.core.designsystem.component.PtButtonStyle
import app.prostuti.core.designsystem.component.SegmentedControl
import app.prostuti.core.designsystem.theme.Spacing
import app.prostuti.core.model.AppLanguage

@Composable
internal fun WelcomeScreen(
    onPhone: () -> Unit,
    onEmail: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 520.dp).fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = Spacing.xxl, vertical = Spacing.xl),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark()
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.auth_brand), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(48.dp))
            Text(
                stringResource(R.string.auth_tagline),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(Spacing.xxxl))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                Point(Icons.Rounded.CalendarMonth, stringResource(R.string.auth_point_plan))
                Point(Icons.Rounded.Timer, stringResource(R.string.auth_point_mock))
                Point(Icons.Rounded.CloudDownload, stringResource(R.string.auth_point_offline))
            }
            Spacer(Modifier.weight(1f).height(48.dp))
            Text(stringResource(R.string.auth_language), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Spacing.sm))
            SegmentedControl(
                options = listOf(AppLanguage.BENGALI, AppLanguage.ENGLISH),
                selected = language,
                onSelect = viewModel::setLanguage,
                label = { if (it == AppLanguage.BENGALI) "বাংলা" else "English" },
            )
            Spacer(Modifier.height(Spacing.xl))
            PtButton(stringResource(R.string.auth_continue_phone), onPhone, Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.sm))
            PtButton(stringResource(R.string.auth_use_email), onEmail, Modifier.fillMaxWidth(), style = PtButtonStyle.Text)
            Spacer(Modifier.height(Spacing.md))
            Text(
                stringResource(R.string.auth_terms),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Point(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, size = 40.dp)
        Spacer(Modifier.width(Spacing.lg))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Wordmark tile: the Bengali letter "প" on emerald. */
@Composable
internal fun BrandMark(size: androidx.compose.ui.unit.Dp = 36.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text("প", color = MaterialTheme.colorScheme.onPrimary, fontSize = (size.value * 0.55f).sp, fontWeight = FontWeight.Bold)
    }
}
