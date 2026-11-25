package app.prostuti.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.prostuti.core.designsystem.theme.Spacing

/**
 * Standard screen frame: back arrow, title, actions, snackbar host and an optional bottom bar.
 * Content is width-capped so tablets and foldables get a readable column, not stretched cards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PtScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backContentDescription: String = "Back",
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge)
                        if (subtitle != null) {
                            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, backContentDescription) }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        containerColor = MaterialTheme.colorScheme.background,
        content = content,
    )
}

/** Centers content and caps its width on large screens. */
@Composable
fun ReadableWidth(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = Spacing.contentMaxWidth).fillMaxWidth()) { content() }
    }
}

val ScreenPadding = PaddingValues(horizontal = Spacing.gutter, vertical = Spacing.lg)

fun Modifier.screenGutter(): Modifier = padding(horizontal = Spacing.gutter)
