package app.prostuti.feature.mockexam

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import app.prostuti.core.designsystem.theme.ProstutiThemeExt
import app.prostuti.core.designsystem.theme.Spacing
import app.prostuti.core.designsystem.theme.tabular
import app.prostuti.core.domain.exam.NegativeMarking
import app.prostuti.core.model.*
import app.prostuti.core.ui.*
import java.time.ZoneId

@Composable
internal fun MockListScreen(onOpen: (String) -> Unit, viewModel: MockListViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    var fullLength by rememberSaveable { mutableStateOf(true) }
    PtScaffold(
        stringResource(R.string.mk_title), onBack = { nav.navigate(Destination.Back) },
        actions = { IconButton(onClick = { nav.navigate(Destination.MockHistory) }) { Icon(Icons.Rounded.History, stringResource(R.string.mk_history)) } },
    ) { padding ->
        StateContent(state, onRetry = viewModel::load, modifier = Modifier.padding(padding)) { mocks ->
            ReadableWidth {
                LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    item {
                        SegmentedControl(listOf(true, false), fullLength, { fullLength = it }, label = { stringResource(if (it) R.string.mk_full else R.string.mk_sectional) })
                    }
                    val visible = mocks.filter { it.isFullLength == fullLength }
                    if (visible.isEmpty()) item { EmptyState(Icons.Rounded.Timer, stringResource(R.string.mk_empty_title), stringResource(R.string.mk_empty_body)) }
                    items(visible, key = { it.id }) { m -> MockCard(m) { onOpen(m.id) } }
                }
            }
        }
    }
}

@Composable
private fun MockCard(m: MockExamSummary, onClick: () -> Unit) {
    val ext = ProstutiThemeExt.colors
    PtCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(m.examShortName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(m.title.text(), style = MaterialTheme.typography.titleSmall)
            }
            if (m.isPremium) Pill(stringResource(R.string.mk_plus), color = ext.community, container = ext.communityContainer, icon = Icons.Rounded.Lock)
        }
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.mk_meta, m.questionCount.loc(), minutesLabel(m.durationMinutes), m.totalMarks.loc()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            m.sections.firstOrNull()?.negativeMarkPerWrong?.takeIf { it > 0 }?.let { Pill(stringResource(R.string.mk_negative, it.loc()), color = ext.incorrect, container = ext.incorrectContainer) }
            if (m.attemptCount > 0) Pill(stringResource(R.string.mk_attempts, m.attemptCount.loc()))
            m.bestScore?.let { Pill(stringResource(R.string.mk_best, it.loc()), color = ext.correct, container = ext.correctContainer) }
        }
    }
}

@Composable
internal fun MockInstructionsScreen(onStart: (String) -> Unit, viewModel: MockInstructionsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    PtScaffold(stringResource(R.string.mk_instructions), onBack = { nav.navigate(Destination.Back) }) { padding ->
        StateContent(state, onRetry = viewModel::load, modifier = Modifier.padding(padding)) { m ->
            val negative = m.sections.maxOfOrNull { it.negativeMarkPerWrong } ?: 0.0
            val marks = m.sections.firstOrNull()?.marksPerQuestion ?: 1.0
            // Smallest number of remaining options at which guessing has positive expected value.
            val mustEliminate = (4 downTo 2).firstOrNull { remaining -> NegativeMarking.isGuessWorthIt(remaining, marks, negative) }?.let { 4 - it }
            ReadableWidth {
                Column(Modifier.padding(padding).fillMaxSize().padding(Spacing.gutter)) {
                    Text(m.title.text(), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.mk_meta, m.questionCount.loc(), minutesLabel(m.durationMinutes), m.totalMarks.loc()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(Spacing.xl))
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg), modifier = Modifier.weight(1f)) {
                        Rule(Icons.Rounded.Timer, stringResource(R.string.mk_rule_time, m.durationMinutes.loc()))
                        Rule(Icons.Rounded.ViewAgenda, stringResource(if (m.rules.allowSectionSwitching) R.string.mk_rule_sections else R.string.mk_rule_sections_locked, m.sections.size.loc()))
                        if (negative > 0) {
                            Rule(Icons.Rounded.RemoveCircleOutline, stringResource(R.string.mk_rule_negative, negative.loc()))
                            mustEliminate?.takeIf { it > 0 }?.let { Rule(Icons.Rounded.Lightbulb, stringResource(R.string.mk_rule_guess, it.loc())) }
                        } else Rule(Icons.Rounded.CheckCircle, stringResource(R.string.mk_rule_no_negative))
                        Rule(Icons.Rounded.CloudDone, stringResource(R.string.mk_rule_autosave))
                        if (!m.rules.revealAnswersDuringExam) Rule(Icons.Rounded.VisibilityOff, stringResource(R.string.mk_rule_reveal))
                        if (m.rules.autoSubmitOnTimeout) Rule(Icons.Rounded.Send, stringResource(R.string.mk_rule_auto_submit))
                        Rule(Icons.Rounded.Wifi, stringResource(R.string.mk_rule_online))
                    }
                    if (m.isPremium) PtButton(stringResource(R.string.mk_upgrade), { nav.navigate(Destination.Subscription) }, Modifier.fillMaxWidth().navigationBarsPadding(), leadingIcon = Icons.Rounded.Lock)
                    else PtButton(stringResource(R.string.mk_start), { onStart(m.id) }, Modifier.fillMaxWidth().navigationBarsPadding())
                }
            }
        }
    }
}

@Composable
private fun Rule(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Spacing.md))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

private enum class ReviewFilter { ALL, WRONG, SKIPPED }

@Composable
internal fun MockResultScreen(viewModel: MockResultViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    val ext = ProstutiThemeExt.colors
    val lang = LocalAppLanguage.current
    var filter by rememberSaveable { mutableStateOf(ReviewFilter.ALL) }
    var expanded by rememberSaveable { mutableStateOf(setOf<String>()) }
    PtScaffold(stringResource(R.string.mr_title), onBack = { nav.navigate(Destination.Back) }) { padding ->
        StateContent(state, onRetry = viewModel::load, modifier = Modifier.padding(padding)) { r ->
            ReadableWidth {
                LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    item {
                        PtCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, border = false, contentPadding = PaddingValues(Spacing.xl)) {
                            val on = MaterialTheme.colorScheme.onPrimary
                            Text(r.mock.title.text(), style = MaterialTheme.typography.labelLarge, color = on.copy(alpha = 0.85f))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(r.score.loc(), style = MaterialTheme.typography.displaySmall.tabular, color = on)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.mr_out_of, r.maxScore.loc()), style = MaterialTheme.typography.titleSmall, color = on.copy(alpha = 0.85f), modifier = Modifier.padding(bottom = 6.dp))
                            }
                            Text(stringResource(R.string.mr_net_score), style = MaterialTheme.typography.labelMedium, color = on.copy(alpha = 0.85f))
                            Spacer(Modifier.height(Spacing.md))
                            r.percentile?.let { Text(stringResource(R.string.mr_percentile, percent(it / 100f)), style = MaterialTheme.typography.bodyMedium, color = on) }
                            if (r.rank != null && r.totalCandidates != null) Text(stringResource(R.string.mr_rank, r.rank!!.loc(), r.totalCandidates!!.loc()), style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.85f))
                            Text(stringResource(R.string.mr_time_taken, r.timeTaken.clock(lang)), style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.85f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            PtCard(Modifier.weight(1f)) {
                                StatBlock("−" + r.negativeMarks.loc(), stringResource(R.string.mr_negative), valueColor = ext.incorrect)
                            }
                            r.passMarks?.let { pass ->
                                PtCard(Modifier.weight(1f)) {
                                    val gap = pass - r.score
                                    StatBlock(pass.loc(), stringResource(R.string.mr_pass_label))
                                    Text(if (gap <= 0) stringResource(R.string.mr_passed) else stringResource(R.string.mr_failed, gap.loc()),
                                        style = MaterialTheme.typography.labelMedium, color = if (gap <= 0) ext.correct else ext.incorrect)
                                }
                            }
                        }
                    }
                    item {
                        PtCard(Modifier.fillMaxWidth()) {
                            SegmentedMeter(listOf(r.correct.toFloat() to ext.correct, r.incorrect.toFloat() to ext.incorrect, r.skipped.toFloat() to MaterialTheme.colorScheme.outlineVariant))
                            Spacer(Modifier.height(Spacing.sm))
                            Text(stringResource(R.string.mr_section_row, r.correct.loc(), r.incorrect.loc(), r.skipped.loc()), style = MaterialTheme.typography.labelLarge.tabular)
                        }
                    }
                    item { SectionHeader(stringResource(R.string.mr_sections)) }
                    item {
                        PtCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                            r.sectionResults.forEachIndexed { i, s ->
                                if (i > 0) HairlineDivider()
                                Column(Modifier.padding(Spacing.lg)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(s.section.name.text(), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                        Text("${s.score.loc()} / ${s.maxScore.loc()}", style = MaterialTheme.typography.titleSmall.tabular)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    MeterBar(if (s.maxScore == 0.0) 0f else (s.score / s.maxScore).toFloat().coerceIn(0f, 1f))
                                    Spacer(Modifier.height(6.dp))
                                    Row {
                                        Text(stringResource(R.string.mr_section_row, s.correct.loc(), s.incorrect.loc(), s.skipped.loc()), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                                        if (s.negativeMarks > 0) Text("−" + s.negativeMarks.loc(), style = MaterialTheme.typography.labelMedium, color = ext.incorrect)
                                    }
                                }
                            }
                        }
                    }
                    if (r.recommendations.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.mr_what_next)) }
                        items(r.recommendations, key = { it.id }) { rec -> RecommendationCard(rec, onClick = { nav.navigate(rec.action.destination()) }) }
                    }
                    item {
                        SectionHeader(stringResource(R.string.mr_review))
                        Spacer(Modifier.height(Spacing.sm))
                        SegmentedControl(ReviewFilter.entries, filter, { filter = it }, label = {
                            stringResource(when (it) { ReviewFilter.ALL -> R.string.mr_filter_all; ReviewFilter.WRONG -> R.string.mr_filter_wrong; ReviewFilter.SKIPPED -> R.string.mr_filter_skipped })
                        })
                    }
                    val reviewed = r.questions.mapIndexed { i, q -> i to q }.filter { (_, q) ->
                        val sel = r.answers[q.id].orEmpty()
                        when (filter) {
                            ReviewFilter.ALL -> true
                            ReviewFilter.SKIPPED -> sel.isEmpty()
                            ReviewFilter.WRONG -> sel.isNotEmpty() && !NegativeMarking.isAnswerCorrect(sel, q.solution?.correctOptionIds.orEmpty())
                        }
                    }
                    items(reviewed, key = { it.second.id }) { (i, q) ->
                        val sel = r.answers[q.id].orEmpty()
                        val ok = NegativeMarking.isAnswerCorrect(sel, q.solution?.correctOptionIds.orEmpty())
                        val open = q.id in expanded
                        PtCard(Modifier.fillMaxWidth(), onClick = { expanded = if (open) expanded - q.id else expanded + q.id }) {
                            Row {
                                Icon(when { sel.isEmpty() -> Icons.Rounded.RemoveCircleOutline; ok -> Icons.Rounded.CheckCircle; else -> Icons.Rounded.Cancel }, null,
                                    tint = when { sel.isEmpty() -> MaterialTheme.colorScheme.outline; ok -> ext.correct; else -> ext.incorrect }, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(Spacing.sm))
                                Text("${(i + 1).loc()}. " + q.stem.text(), style = MaterialTheme.typography.bodyMedium, maxLines = if (open) Int.MAX_VALUE else 2)
                            }
                            if (open) {
                                Spacer(Modifier.height(Spacing.md))
                                QuestionView(q, sel, onSelect = null, revealed = true, showMeta = true)
                                Spacer(Modifier.height(Spacing.md))
                                SolutionView(q)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MockHistoryScreen(viewModel: MockHistoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    val ext = ProstutiThemeExt.colors
    PtScaffold(stringResource(R.string.mr_history_title), onBack = { nav.navigate(Destination.Back) }) { padding ->
        StateContent(state, onRetry = viewModel::load, modifier = Modifier.padding(padding)) { list ->
            if (list.isEmpty()) EmptyState(Icons.Rounded.History, stringResource(R.string.mr_history_title), stringResource(R.string.mr_history_empty))
            else ReadableWidth {
                LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    if (list.size >= 2) item {
                        PtCard(Modifier.fillMaxWidth()) {
                            TrendLine(list.reversed().map { (it.score / it.maxScore).toFloat() }, Modifier.fillMaxWidth().height(80.dp), minValue = 0f, maxValue = 1f)
                        }
                    }
                    itemsIndexed(list, key = { _, it -> it.attemptId }) { _, a ->
                        PtCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Destination.MockResult(a.attemptId)) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(a.mockTitle.text(), style = MaterialTheme.typography.titleSmall)
                                    Text(a.submittedAt.atZone(ZoneId.systemDefault()).toLocalDate().pretty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${a.score.loc()} / ${a.maxScore.loc()}", style = MaterialTheme.typography.titleSmall.tabular)
                                    if (a.negativeMarks > 0) Text("−" + a.negativeMarks.loc(), style = MaterialTheme.typography.labelSmall, color = ext.incorrect)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
