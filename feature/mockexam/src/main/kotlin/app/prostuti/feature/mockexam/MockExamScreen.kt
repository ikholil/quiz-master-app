package app.prostuti.feature.mockexam

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.prostuti.core.designsystem.component.*
import app.prostuti.core.designsystem.theme.ProstutiThemeExt
import app.prostuti.core.designsystem.theme.Spacing
import app.prostuti.core.designsystem.theme.tabular
import app.prostuti.core.domain.exam.ExamSessionState
import app.prostuti.core.domain.exam.QuestionStatus
import app.prostuti.core.ui.*
import java.time.Duration

@Composable
internal fun MockExamScreen(viewModel: MockExamViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.saveNow() }
    // Keep the screen awake during an exam.
    val view = LocalView.current
    DisposableEffect(view) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }
    LaunchedEffect(state.phase) {
        (state.phase as? ExamPhase.Submitted)?.let { nav.navigate(Destination.MockResult(it.attemptId)) }
    }
    MockExamContent(
        state = state,
        onSelect = viewModel::select,
        onClear = viewModel::clear,
        onReview = viewModel::toggleReview,
        onNext = viewModel::next,
        onPrevious = viewModel::previous,
        onGoTo = viewModel::goTo,
        onSection = viewModel::goToSection,
        onSubmit = { viewModel.submit() },
        onRetryStart = { viewModel.start() },
        onLeave = { viewModel.saveNow(); nav.navigate(Destination.Back) },
    )
}

/** Stateless exam UI — also driven directly by UI tests. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockExamContent(
    state: MockExamUiState,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
    onReview: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onGoTo: (String) -> Unit,
    onSection: (Int) -> Unit,
    onSubmit: () -> Unit,
    onRetryStart: () -> Unit,
    onLeave: () -> Unit,
) {
    var confirmSubmit by rememberSaveable { mutableStateOf(false) }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = state.phase == ExamPhase.Running) { confirmLeave = true }

    when (val phase = state.phase) {
        ExamPhase.Starting -> { Scaffold { SkeletonList(Modifier.padding(it)) }; return }
        is ExamPhase.Failed -> { Scaffold { ErrorView(phase.error, onRetryStart, Modifier.padding(it)) }; return }
        else -> Unit
    }
    val session = state.session ?: return
    val attempt = state.attempt ?: return
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                Row(Modifier.padding(horizontal = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { confirmLeave = true }) { Icon(Icons.Rounded.Close, stringResource(R.string.mk_leave)) }
                    Column(Modifier.weight(1f)) {
                        Text(attempt.mock.title.text(), style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        SaveIndicator(state.saveStatus)
                    }
                    TimerPill(state.remaining)
                    IconButton(onClick = { sheetOpen = true }, modifier = Modifier.testTag("answerSheet")) {
                        BadgedBox(badge = { Badge { Text(session.answeredCount.loc()) } }) { Icon(Icons.Rounded.GridView, stringResource(R.string.mk_answer_sheet)) }
                    }
                }
                if (session.sections.size > 1) {
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.lg, vertical = Spacing.xs), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        session.sections.forEachIndexed { i, s ->
                            val answered = s.questions.count { session.answers[it.id].orEmpty().isNotEmpty() }
                            PtFilterChip("${s.section.name.text()} ${answered.loc()}/${s.questions.size.loc()}", i == session.sectionIndex, { onSection(i) })
                        }
                    }
                }
                MeterBar((session.globalIndex + 1f) / session.totalQuestions, height = 3.dp)
            }
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Spacing.md, vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    IconButton(onClick = onPrevious, enabled = !session.isFirst) { Icon(Icons.Rounded.ChevronLeft, stringResource(R.string.mk_previous)) }
                    PtButton(stringResource(R.string.mk_clear), onClear, style = PtButtonStyle.Text, enabled = session.answers[session.currentQuestion.id].orEmpty().isNotEmpty())
                    Spacer(Modifier.weight(1f))
                    if (session.isLast) PtButton(stringResource(R.string.mk_submit), { confirmSubmit = true }, modifier = Modifier.testTag("submit"))
                    else PtButton(stringResource(R.string.mk_next), onNext, modifier = Modifier.testTag("next"), trailingIcon = Icons.Rounded.ChevronRight)
                }
            }
        },
    ) { padding ->
        val q = session.currentQuestion
        val marked = q.id in session.markedForReview
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.gutter)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.mk_question_n, (session.globalIndex + 1).loc()) + " / " + session.totalQuestions.loc(), style = MaterialTheme.typography.labelLarge.tabular, modifier = Modifier.weight(1f))
                val ext = ProstutiThemeExt.colors
                FilterChip(
                    selected = marked, onClick = onReview,
                    label = { Text(stringResource(if (marked) R.string.mk_marked else R.string.mk_mark_review)) },
                    leadingIcon = { Icon(if (marked) Icons.Rounded.Flag else Icons.Rounded.OutlinedFlag, null, Modifier.size(18.dp)) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ext.reviewContainer, selectedLabelColor = ext.review, selectedLeadingIconColor = ext.review),
                    modifier = Modifier.testTag("markReview"),
                )
            }
            Spacer(Modifier.height(Spacing.md))
            // revealed = false: answers are never shown during an active exam.
            QuestionView(q, session.answers[q.id].orEmpty(), onSelect = onSelect, revealed = false, showMeta = false)
            if (session.currentSection.section.negativeMarkPerWrong > 0) {
                Spacer(Modifier.height(Spacing.lg))
                Text(stringResource(R.string.mk_negative_short, session.currentSection.section.negativeMarkPerWrong.loc()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (sheetOpen) ModalBottomSheet(onDismissRequest = { sheetOpen = false }, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        AnswerSheet(session, onPick = { onGoTo(it); sheetOpen = false }, onSubmit = { sheetOpen = false; confirmSubmit = true })
    }
    if (confirmSubmit) AlertDialog(
        onDismissRequest = { confirmSubmit = false },
        title = { Text(stringResource(R.string.mk_submit_title)) },
        text = {
            Column {
                Text(stringResource(R.string.mk_submit_body, session.answeredCount.loc(), session.unansweredCount.loc(), session.markedCount.loc()))
                Spacer(Modifier.height(Spacing.sm))
                Text(stringResource(R.string.mk_submit_warning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton({ confirmSubmit = false; onSubmit() }, Modifier.testTag("confirmSubmit")) { Text(stringResource(R.string.mk_submit_confirm)) } },
        dismissButton = { TextButton({ confirmSubmit = false }) { Text(stringResource(R.string.mk_keep_working)) } },
    )
    if (confirmLeave) AlertDialog(
        onDismissRequest = { confirmLeave = false },
        title = { Text(stringResource(R.string.mk_leave_title)) },
        text = { Text(stringResource(R.string.mk_leave_body)) },
        confirmButton = { TextButton({ confirmLeave = false; onLeave() }) { Text(stringResource(R.string.mk_leave)) } },
        dismissButton = { TextButton({ confirmLeave = false }) { Text(stringResource(R.string.mk_keep_working)) } },
    )
    when (val phase = state.phase) {
        is ExamPhase.Submitting -> BlockingOverlay(stringResource(if (phase.timeUp) R.string.mk_time_up else R.string.mk_submitting))
        is ExamPhase.SubmitFailed -> AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.mk_submit_failed)) },
            text = { Text(errorMessage(phase.error)) },
            confirmButton = { TextButton(onSubmit) { Text(stringResource(R.string.mk_retry)) } },
        )
        else -> Unit
    }
}

@Composable
private fun SaveIndicator(status: SaveStatus) {
    val (text, color) = when (status) {
        SaveStatus.IDLE -> return
        SaveStatus.SAVING -> stringResource(R.string.mk_saving) to MaterialTheme.colorScheme.onSurfaceVariant
        SaveStatus.SAVED -> stringResource(R.string.mk_saved) to ProstutiThemeExt.colors.correct
        SaveStatus.FAILED -> stringResource(R.string.mk_save_failed) to ProstutiThemeExt.colors.incorrect
    }
    Text(text, style = MaterialTheme.typography.labelSmall, color = color)
}

@Composable
private fun TimerPill(remaining: Duration) {
    val ext = ProstutiThemeExt.colors
    val lang = LocalAppLanguage.current
    val urgent = remaining.seconds <= 300
    val label = stringResource(R.string.mk_time_left)
    val oneMinute = stringResource(R.string.mk_one_minute)
    Row(
        Modifier.clip(CircleShape).background(if (urgent) ext.incorrectContainer else MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .semantics {
                contentDescription = "$label ${remaining.toMinutes()} min"
                if (remaining.seconds in 1..60) { liveRegion = LiveRegionMode.Polite; contentDescription = oneMinute }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Timer, null, Modifier.size(16.dp), tint = if (urgent) ext.incorrect else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(remaining.clock(lang), style = MaterialTheme.typography.labelLarge.tabular, color = if (urgent) ext.incorrect else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
internal fun statusColors(status: QuestionStatus): Pair<Color, Color> {
    val ext = ProstutiThemeExt.colors
    val scheme = MaterialTheme.colorScheme
    return when (status) {
        QuestionStatus.ANSWERED -> ext.correct to Color.White
        QuestionStatus.ANSWERED_AND_MARKED -> ext.review to Color.White
        QuestionStatus.MARKED_FOR_REVIEW -> ext.reviewContainer to ext.review
        QuestionStatus.NOT_ANSWERED -> ext.incorrectContainer to ext.incorrect
        QuestionStatus.NOT_VISITED -> scheme.surfaceVariant to scheme.onSurfaceVariant
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnswerSheet(session: ExamSessionState, onPick: (String) -> Unit, onSubmit: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Spacing.gutter).padding(bottom = Spacing.xxl).navigationBarsPadding()) {
        Text(stringResource(R.string.mk_answer_sheet), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(Spacing.md))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                QuestionStatus.ANSWERED to R.string.mk_status_answered,
                QuestionStatus.NOT_ANSWERED to R.string.mk_status_unanswered,
                QuestionStatus.MARKED_FOR_REVIEW to R.string.mk_status_review,
                QuestionStatus.ANSWERED_AND_MARKED to R.string.mk_status_answered_review,
                QuestionStatus.NOT_VISITED to R.string.mk_status_not_visited,
            ).forEach { (st, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(statusColors(st).first, size = 12.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        var number = 0
        session.sections.forEach { section ->
            Spacer(Modifier.height(Spacing.lg))
            Text(section.section.name.text(), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(Spacing.sm))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                section.questions.forEach { q ->
                    number++
                    val status = session.statusOf(q.id)
                    val (bg, fg) = statusColors(status)
                    val current = q.id == session.currentQuestion.id
                    val statusLabel = stringResource(
                        when (status) {
                            QuestionStatus.ANSWERED -> R.string.mk_status_answered
                            QuestionStatus.NOT_ANSWERED -> R.string.mk_status_unanswered
                            QuestionStatus.MARKED_FOR_REVIEW -> R.string.mk_status_review
                            QuestionStatus.ANSWERED_AND_MARKED -> R.string.mk_status_answered_review
                            QuestionStatus.NOT_VISITED -> R.string.mk_status_not_visited
                        },
                    )
                    val n = number
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(bg)
                            .then(if (current) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                            .clickable { onPick(q.id) }
                            .semantics { contentDescription = "$n, $statusLabel" },
                        contentAlignment = Alignment.Center,
                    ) { Text(n.loc(), style = MaterialTheme.typography.labelLarge.tabular, color = fg) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.xl))
        PtButton(stringResource(R.string.mk_submit), onSubmit, Modifier.fillMaxWidth())
    }
}

@Composable
private fun BlockingOverlay(message: String) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)).clickable(enabled = true, onClick = {}), contentAlignment = Alignment.Center) {
        PtCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                Spacer(Modifier.width(Spacing.lg))
                Text(message, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
