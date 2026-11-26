package app.prostuti.feature.practice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.prostuti.core.designsystem.component.*
import app.prostuti.core.designsystem.theme.Spacing
import app.prostuti.core.model.Difficulty
import app.prostuti.core.model.PracticeMode
import app.prostuti.core.ui.*

private data class ModeCard(val mode: PracticeMode, val icon: ImageVector, val title: Int, val desc: Int)

private val modes = listOf(
    ModeCard(PracticeMode.WEAK_TOPICS, Icons.Rounded.TrackChanges, R.string.pr_mode_weak, R.string.pr_mode_weak_desc),
    ModeCard(PracticeMode.WRONG_ANSWERS, Icons.Rounded.Replay, R.string.pr_mode_wrong, R.string.pr_mode_wrong_desc),
    ModeCard(PracticeMode.SPEED, Icons.Rounded.Bolt, R.string.pr_mode_speed, R.string.pr_mode_speed_desc),
    ModeCard(PracticeMode.ACCURACY, Icons.Rounded.CenterFocusStrong, R.string.pr_mode_accuracy, R.string.pr_mode_accuracy_desc),
    ModeCard(PracticeMode.MIXED, Icons.Rounded.Shuffle, R.string.pr_mode_mixed, R.string.pr_mode_mixed_desc),
    ModeCard(PracticeMode.PREVIOUS_YEAR, Icons.Rounded.History, R.string.pr_mode_pyq, R.string.pr_mode_pyq_desc),
    ModeCard(PracticeMode.MARATHON, Icons.Rounded.DirectionsRun, R.string.pr_mode_marathon, R.string.pr_mode_marathon_desc),
    ModeCard(PracticeMode.TOPIC, Icons.Rounded.AccountTree, R.string.pr_mode_topic, R.string.pr_mode_topic_desc),
)

@Composable
internal fun PracticeHubScreen(onStart: (PracticeSessionRoute) -> Unit, viewModel: PracticeHubViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    var customOpen by rememberSaveable { mutableStateOf(false) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        ReadableWidth {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(Spacing.gutter),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) { Text(stringResource(R.string.pr_title), style = MaterialTheme.typography.headlineSmall) }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PtCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, border = false, onClick = { onStart(PracticeSessionRoute(PracticeMode.DAILY_CHALLENGE.name)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.pr_daily), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
                                Text(stringResource(R.string.pr_daily_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                            }
                            IconBadge(Icons.Rounded.EmojiEvents, tint = MaterialTheme.colorScheme.primary, container = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PtCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Destination.MockList()) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Rounded.Timer)
                            Spacer(Modifier.width(Spacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.pr_mock_tests), style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(R.string.pr_mock_tests_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Rounded.ChevronRight, null)
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader(stringResource(R.string.pr_modes)) }
                items(modes, key = { it.mode }) { m ->
                    PtCard(Modifier.fillMaxWidth().heightIn(min = 132.dp), onClick = {
                        if (m.mode == PracticeMode.TOPIC) nav.navigate(Destination.Syllabus(null)) else onStart(PracticeSessionRoute(m.mode.name))
                    }) {
                        IconBadge(m.icon, size = 36.dp)
                        Spacer(Modifier.height(Spacing.sm))
                        Text(stringResource(m.title), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(m.desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PtCard(Modifier.fillMaxWidth(), onClick = { customOpen = true }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Rounded.Tune)
                            Spacer(Modifier.width(Spacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.pr_mode_custom), style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(R.string.pr_mode_custom_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PtButton(stringResource(R.string.pr_question_bank), { nav.navigate(Destination.QuestionBank(state.examId)) }, style = PtButtonStyle.Text, leadingIcon = Icons.Rounded.Quiz)
                }
            }
        }
    }
    if (customOpen) CustomPracticeSheet(state, onDismiss = { customOpen = false }) { customOpen = false; onStart(it) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CustomPracticeSheet(state: HubState, onDismiss: () -> Unit, onStart: (PracticeSessionRoute) -> Unit) {
    var subjects by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var count by rememberSaveable { mutableFloatStateOf(20f) }
    var difficulty by rememberSaveable { mutableStateOf<String?>(null) }
    var minutes by rememberSaveable { mutableIntStateOf(0) }
    var feedback by rememberSaveable { mutableStateOf(true) }
    var offline by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Spacing.gutter).padding(bottom = Spacing.xxl).navigationBarsPadding()) {
            Text(stringResource(R.string.pr_custom_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Spacing.lg))
            Text(stringResource(R.string.pr_subjects), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(Spacing.sm))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PtFilterChip(stringResource(R.string.pr_all_subjects), subjects.isEmpty(), { subjects = emptyList() })
                state.subjects.forEach { s ->
                    PtFilterChip(s.name.text(), s.id in subjects, { subjects = if (s.id in subjects) subjects - s.id else subjects + s.id })
                }
            }
            Spacer(Modifier.height(Spacing.lg))
            Text(stringResource(R.string.pr_count, count.toInt().loc()), style = MaterialTheme.typography.labelLarge)
            Slider(count, { count = it }, valueRange = 5f..100f, steps = 18)
            Text(stringResource(R.string.pr_difficulty), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(Spacing.sm))
            SegmentedControl(listOf<String?>(null, Difficulty.EASY.name, Difficulty.MEDIUM.name, Difficulty.HARD.name), difficulty, { difficulty = it }, label = {
                when (it) {
                    null -> stringResource(R.string.pr_any)
                    Difficulty.EASY.name -> stringResource(app.prostuti.core.ui.R.string.ui_difficulty_easy)
                    Difficulty.MEDIUM.name -> stringResource(app.prostuti.core.ui.R.string.ui_difficulty_medium)
                    else -> stringResource(app.prostuti.core.ui.R.string.ui_difficulty_hard)
                }
            })
            Spacer(Modifier.height(Spacing.lg))
            Text(stringResource(R.string.pr_timer), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(Spacing.sm))
            SegmentedControl(listOf(0, 10, 20, 30, 60), minutes, { minutes = it }, label = { if (it == 0) stringResource(R.string.pr_no_timer) else it.loc() + "′" })
            Spacer(Modifier.height(Spacing.lg))
            SwitchRow(stringResource(R.string.pr_feedback), stringResource(R.string.pr_feedback_desc), feedback) { feedback = it }
            SwitchRow(stringResource(R.string.pr_offline_only), null, offline) { offline = it }
            Spacer(Modifier.height(Spacing.lg))
            PtButton(stringResource(R.string.pr_start), {
                onStart(PracticeSessionRoute(PracticeMode.CUSTOM.name, subjectIds = subjects, count = count.toInt(), difficulty = difficulty, minutes = minutes, instantFeedback = feedback, offlineOnly = offline))
            }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onChange)
    }
}
