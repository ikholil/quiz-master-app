package app.prostuti.feature.practice

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.prostuti.core.designsystem.component.*
import app.prostuti.core.designsystem.theme.ProstutiThemeExt
import app.prostuti.core.designsystem.theme.Spacing
import app.prostuti.core.designsystem.theme.tabular
import app.prostuti.core.domain.exam.NegativeMarking
import app.prostuti.core.model.*
import app.prostuti.core.ui.*
import java.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PracticeSessionScreen(viewModel: PracticeSessionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    val active = state.phase == SessionPhase.Active
    BackHandler(enabled = active) { confirmExit = true }

    when (val phase = state.phase) {
        SessionPhase.Loading -> Scaffold { SkeletonList(Modifier.padding(it)) }
        is SessionPhase.Failed -> Scaffold { ErrorView(phase.error, viewModel::start, Modifier.padding(it)) }
        is SessionPhase.Finished -> PracticeResultContent(phase.result)
        SessionPhase.Active, SessionPhase.Submitting -> {
            val q = state.current ?: return
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    Column(Modifier.statusBarsPadding()) {
                        Row(Modifier.padding(horizontal = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { confirmExit = true }) { Icon(Icons.Rounded.Close, stringResource(R.string.pr_leave)) }
                            Text(stringResource(R.string.pr_question_of, (state.index + 1).loc(), state.questions.size.loc()), style = MaterialTheme.typography.titleSmall.tabular, modifier = Modifier.weight(1f))
                            val lang = LocalAppLanguage.current
                            val seconds = state.remainingSeconds ?: state.elapsedSeconds
                            val urgent = state.remainingSeconds != null && state.remainingSeconds!! <= 60
                            Pill(
                                Duration.ofSeconds(seconds).clock(lang),
                                icon = Icons.Rounded.Timer,
                                color = if (urgent) ProstutiThemeExt.colors.incorrect else MaterialTheme.colorScheme.onSurfaceVariant,
                                container = if (urgent) ProstutiThemeExt.colors.incorrectContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(end = Spacing.md),
                            )
                        }
                        MeterBar((state.index + 1f) / state.questions.size, height = 3.dp)
                        if (state.session?.isOffline == true) {
                            Text(stringResource(R.string.pr_offline_session), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(horizontal = Spacing.gutter, vertical = 4.dp))
                        }
                    }
                },
                bottomBar = { SessionBottomBar(state, viewModel) },
            ) { padding ->
                AnimatedContent(state.index, transitionSpec = { (slideInHorizontally { it / 5 } + fadeIn()) togetherWith fadeOut() }, label = "q", modifier = Modifier.padding(padding)) { index ->
                    val question = state.questions[index]
                    ReadableWidth {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.gutter)) {
                            val revealed = question.id in state.checked
                            QuestionView(question, state.answers[question.id].orEmpty(), onSelect = viewModel::select, revealed = revealed)
                            AnimatedVisibility(revealed) {
                                Column(Modifier.padding(top = Spacing.lg)) {
                                    val correct = NegativeMarking.isAnswerCorrect(state.answers[question.id].orEmpty(), question.solution?.correctOptionIds.orEmpty())
                                    if (question.options.isNotEmpty()) FeedbackBanner(correct)
                                    Spacer(Modifier.height(Spacing.md))
                                    SolutionView(question)
                                    PtButton(stringResource(app.prostuti.core.ui.R.string.ui_why_option_wrong),
                                        { nav.navigate(Destination.Assistant(questionId = question.id, intent = TutorIntent.WHY_OTHERS_WRONG)) }, style = PtButtonStyle.Text, leadingIcon = Icons.Rounded.AutoAwesome)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (confirmExit) AlertDialog(
        onDismissRequest = { confirmExit = false },
        title = { Text(stringResource(R.string.pr_exit)) },
        text = { Text(stringResource(R.string.pr_exit_body)) },
        confirmButton = { TextButton({ confirmExit = false; nav.navigate(Destination.Back) }) { Text(stringResource(R.string.pr_leave)) } },
        dismissButton = { TextButton({ confirmExit = false }) { Text(stringResource(R.string.pr_stay)) } },
    )
}

@Composable
private fun FeedbackBanner(correct: Boolean) {
    val ext = ProstutiThemeExt.colors
    Row(
        Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (correct) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel, null, tint = if (correct) ext.correct else ext.incorrect)
        Spacer(Modifier.width(Spacing.sm))
        Text(stringResource(if (correct) R.string.pr_correct else R.string.pr_incorrect), style = MaterialTheme.typography.titleMedium, color = if (correct) ext.correct else ext.incorrect)
    }
}

@Composable
private fun SessionBottomBar(state: PracticeUiState, vm: PracticeSessionViewModel) {
    val q = state.current ?: return
    val answered = state.answers[q.id].orEmpty().isNotEmpty()
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Spacing.lg, vertical = Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            if (!state.instantFeedback && state.index > 0) PtButton(stringResource(R.string.pr_previous), vm::previous, style = PtButtonStyle.Outline)
            val needsCheck = state.instantFeedback && !state.currentChecked && (q.type == QuestionType.MULTI_SELECT || q.options.isEmpty())
            when {
                state.phase == SessionPhase.Submitting -> PtButton(stringResource(R.string.pr_submitting), {}, Modifier.weight(1f), loading = true)
                needsCheck -> {
                    PtButton(stringResource(R.string.pr_skip), vm::next, style = PtButtonStyle.Text)
                    PtButton(if (q.options.isEmpty()) stringResource(R.string.pr_written_reveal) else stringResource(R.string.pr_check), vm::check, Modifier.weight(1f), enabled = answered || q.options.isEmpty())
                }
                state.instantFeedback && !state.currentChecked -> {
                    PtButton(stringResource(R.string.pr_skip), vm::next, Modifier.weight(1f), style = PtButtonStyle.Outline)
                }
                else -> PtButton(
                    stringResource(if (state.isLast) R.string.pr_finish else R.string.pr_next),
                    vm::next, Modifier.weight(1f),
                    style = if (!answered && !state.instantFeedback) PtButtonStyle.Tonal else PtButtonStyle.Primary,
                )
            }
            if (!state.instantFeedback && !state.isLast) PtButton(stringResource(R.string.pr_finish), vm::finish, style = PtButtonStyle.Text)
        }
    }
}

@Composable
internal fun PracticeResultContent(result: PracticeResult) {
    val nav = LocalNavigator.current
    val ext = ProstutiThemeExt.colors
    val lang = LocalAppLanguage.current
    var expanded by rememberSaveable { mutableStateOf(setOf<String>()) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        ReadableWidth {
            LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(Spacing.lg))
                        ProgressRing(result.accuracy, size = 132.dp, stroke = 12.dp, color = if (result.accuracy >= 0.6f) ext.correct else ext.community) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(percent(result.accuracy), style = MaterialTheme.typography.headlineMedium.tabular)
                                Text(stringResource(R.string.rs_accuracy), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(Spacing.md))
                        Text(
                            stringResource(when { result.accuracy >= 0.8f -> R.string.rs_praise_high; result.accuracy >= 0.5f -> R.string.rs_praise_mid; else -> R.string.rs_praise_low }),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        if (!result.isServerVerified) Text(stringResource(R.string.rs_not_verified), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                item {
                    PtCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatBlock("${result.score.loc()}/${result.maxScore.loc()}", stringResource(R.string.rs_score))
                            StatBlock(result.timeSpent.clock(lang), stringResource(R.string.rs_time))
                        }
                        Spacer(Modifier.height(Spacing.lg))
                        SegmentedMeter(listOf(result.correct.toFloat() to ext.correct, result.incorrect.toFloat() to ext.incorrect, result.skipped.toFloat() to MaterialTheme.colorScheme.outlineVariant))
                        Spacer(Modifier.height(Spacing.sm))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                            Legend(ext.correct, stringResource(R.string.rs_correct), result.correct)
                            Legend(ext.incorrect, stringResource(R.string.rs_incorrect), result.incorrect)
                            Legend(MaterialTheme.colorScheme.outline, stringResource(R.string.rs_skipped), result.skipped)
                        }
                    }
                }
                result.nextAction?.let { rec ->
                    item { SectionHeader(stringResource(R.string.rs_next_step)) }
                    item { RecommendationCard(rec, onClick = { nav.navigate(rec.action.destination()) }) }
                }
                if (result.weakTopics.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.rs_weak)) }
                    item {
                        PtCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = Spacing.xs)) {
                            result.weakTopics.forEach { t ->
                                ListRow(t.topicName.text(), subtitle = stringResource(R.string.rs_topic_score, t.correct.loc(), t.total.loc()),
                                    trailing = { Text(percent(t.accuracy), style = MaterialTheme.typography.labelLarge, color = ext.incorrect) },
                                    showChevron = true, onClick = { nav.navigate(Destination.Topic(t.topicId)) })
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        if (result.incorrect > 0) PtButton(stringResource(R.string.rs_retry_wrong), { nav.navigate(Destination.WrongAnswers) }, Modifier.weight(1f), style = PtButtonStyle.Outline)
                        PtButton(stringResource(R.string.rs_done), { nav.navigate(Destination.Back) }, Modifier.weight(1f))
                    }
                }
                item { SectionHeader(stringResource(R.string.rs_review)) }
                itemsIndexed(result.questions, key = { _, q -> q.id }) { i, q ->
                    val selected = result.answers[q.id]?.selectedOptionIds.orEmpty()
                    val correct = NegativeMarking.isAnswerCorrect(selected, q.solution?.correctOptionIds.orEmpty())
                    val open = q.id in expanded
                    PtCard(Modifier.fillMaxWidth(), onClick = { expanded = if (open) expanded - q.id else expanded + q.id }) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                when { selected.isEmpty() -> Icons.Rounded.RemoveCircleOutline; correct -> Icons.Rounded.CheckCircle; else -> Icons.Rounded.Cancel },
                                null, tint = when { selected.isEmpty() -> MaterialTheme.colorScheme.outline; correct -> ext.correct; else -> ext.incorrect },
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(Spacing.sm))
                            Text("${(i + 1).loc()}. " + q.stem.text(), style = MaterialTheme.typography.bodyMedium, maxLines = if (open) Int.MAX_VALUE else 2, modifier = Modifier.weight(1f))
                        }
                        if (open) {
                            Spacer(Modifier.height(Spacing.md))
                            QuestionView(q, selected, onSelect = null, revealed = true, showMeta = false)
                            Spacer(Modifier.height(Spacing.md))
                            SolutionView(q)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Legend(color: androidx.compose.ui.graphics.Color, label: String, value: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color)
        Spacer(Modifier.width(6.dp))
        Text("$label ${value.loc()}", style = MaterialTheme.typography.labelMedium)
    }
}
