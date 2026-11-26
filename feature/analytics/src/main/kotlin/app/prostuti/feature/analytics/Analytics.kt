package app.prostuti.feature.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.prostuti.core.common.AppResult
import app.prostuti.core.designsystem.component.*
import app.prostuti.core.designsystem.theme.ProstutiThemeExt
import app.prostuti.core.designsystem.theme.Spacing
import app.prostuti.core.domain.repository.AnalyticsRepository
import app.prostuti.core.model.AnalyticsRange
import app.prostuti.core.model.PerformanceAnalytics
import app.prostuti.core.ui.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable data object AnalyticsRoute

fun NavGraphBuilder.analyticsGraph(@Suppress("UNUSED_PARAMETER") navController: NavController) {
    composable<AnalyticsRoute> { AnalyticsScreen() }
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(private val repo: AnalyticsRepository) : LoadViewModel<PerformanceAnalytics>() {
    private val _range = MutableStateFlow(AnalyticsRange.MONTH)
    val range = _range.asStateFlow()
    init { load() }
    override suspend fun fetch(): AppResult<PerformanceAnalytics> = repo.analytics(_range.value)
    fun setRange(r: AnalyticsRange) { _range.value = r; load() }
}

@Composable
internal fun AnalyticsScreen(viewModel: AnalyticsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val range by viewModel.range.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    PtScaffold(stringResource(R.string.an_title), onBack = { nav.navigate(Destination.Back) }) { padding ->
        StateContent(state, onRetry = viewModel::load, modifier = Modifier.padding(padding)) { a ->
            ReadableWidth {
                LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    item {
                        SegmentedControl(AnalyticsRange.entries, range, viewModel::setRange, label = {
                            stringResource(when (it) { AnalyticsRange.WEEK -> R.string.an_week; AnalyticsRange.MONTH -> R.string.an_month; AnalyticsRange.ALL -> R.string.an_all })
                        })
                    }
                    if (a.questionsAttempted < 20) {
                        item { EmptyState(Icons.Rounded.Insights, stringResource(R.string.an_no_data), stringResource(R.string.an_no_data_desc)) }
                        return@LazyColumn
                    }
                    item { SummaryGrid(a) }
                    if (a.recommendations.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.an_recommendations)) }
                        items(a.recommendations, key = { it.id }) { RecommendationCard(it, onClick = { nav.navigate(it.action.destination()) }) }
                    }
                    item {
                        SectionHeader(stringResource(R.string.an_accuracy_trend))
                        Spacer(Modifier.height(Spacing.sm))
                        PtCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.width(36.dp)) {
                                    Text(percent(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(44.dp))
                                    Text(percent(0f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TrendLine(a.accuracyTrend.map { it.value }, Modifier.weight(1f).height(90.dp).semantics { contentDescription = a.accuracyTrend.joinToString { "${(it.value * 100).toInt()}%" } }, minValue = 0f, maxValue = 1f)
                            }
                            a.accuracyTrend.lastOrNull()?.let { Text(percent(it.value), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.End)) }
                        }
                    }
                    if (a.mockScoreTrend.size >= 2) item {
                        SectionHeader(stringResource(R.string.an_mock_trend))
                        Spacer(Modifier.height(Spacing.sm))
                        PtCard(Modifier.fillMaxWidth()) {
                            TrendLine(a.mockScoreTrend.map { it.value }, Modifier.fillMaxWidth().height(80.dp), color = ProstutiThemeExt.colors.verified)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(a.mockScoreTrend.first().value.loc(), style = MaterialTheme.typography.labelMedium)
                                Text(a.mockScoreTrend.last().value.loc(), style = MaterialTheme.typography.labelLarge, color = ProstutiThemeExt.colors.verified)
                            }
                        }
                    }
                    item { SectionHeader(stringResource(R.string.an_subjects)) }
                    item {
                        PtCard(Modifier.fillMaxWidth()) {
                            a.subjects.sortedBy { it.accuracy }.forEachIndexed { i, s ->
                                if (i > 0) Spacer(Modifier.height(Spacing.md))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(s.subjectName.text(), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(percent(s.accuracy), style = MaterialTheme.typography.labelLarge, color = masteryColor(s.mastery))
                                }
                                Spacer(Modifier.height(4.dp))
                                MeterBar(s.accuracy, color = masteryColor(s.mastery))
                                Text(stringResource(R.string.an_subject_detail, percent(s.accuracy), s.attempted.loc(), stringResource(R.string.an_seconds, s.avgSecondsPerQuestion.toInt().loc())),
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    item {
                        SectionHeader(stringResource(R.string.an_speed_accuracy), subtitle = stringResource(R.string.an_speed_accuracy_desc))
                        Spacer(Modifier.height(Spacing.sm))
                        SpeedAccuracyChart(a)
                    }
                    if (a.repeatedMistakes.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.an_mistakes)) }
                        items(a.repeatedMistakes, key = { it.topicId }) { m ->
                            PtCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Destination.Topic(m.topicId)) }) {
                                Row(verticalAlignment = Alignment.Top) {
                                    IconBadge(Icons.Rounded.Repeat, tint = ProstutiThemeExt.colors.incorrect, container = ProstutiThemeExt.colors.incorrectContainer, size = 36.dp)
                                    Spacer(Modifier.width(Spacing.md))
                                    Column(Modifier.weight(1f)) {
                                        Text(m.topicName.text(), style = MaterialTheme.typography.titleSmall)
                                        Text(m.pattern.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(stringResource(R.string.an_mistake_count, m.occurrences.loc()), style = MaterialTheme.typography.labelMedium, color = ProstutiThemeExt.colors.incorrect)
                                }
                            }
                        }
                    }
                    item {
                        PtCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProgressRing(a.revisionRetention, size = 56.dp, stroke = 6.dp) { Text(percent(a.revisionRetention), style = MaterialTheme.typography.labelMedium) }
                                Spacer(Modifier.width(Spacing.lg))
                                Column {
                                    Text(stringResource(R.string.an_retention), style = MaterialTheme.typography.titleSmall)
                                    Text(stringResource(R.string.an_retention_desc, percent(a.revisionRetention)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    item {
                        SectionHeader(stringResource(R.string.an_consistency), subtitle = stringResource(R.string.an_consistency_desc))
                        Spacer(Modifier.height(Spacing.sm))
                        ConsistencyGrid(a.consistency)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryGrid(a: PerformanceAnalytics) {
    val tiles = listOf(
        percent(a.overallAccuracy) to stringResource(R.string.an_accuracy),
        a.questionsAttempted.loc() to stringResource(R.string.an_questions),
        stringResource(R.string.an_seconds, a.avgSecondsPerQuestion.toInt().loc()) to stringResource(R.string.an_avg_time),
        ("−" + a.negativeMarksLost.loc()) to stringResource(R.string.an_negative),
        percent(a.syllabusCompletion) to stringResource(R.string.an_syllabus),
        a.studyDaysLast30.loc() to stringResource(R.string.an_study_days),
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        tiles.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                row.forEachIndexed { i, (v, l) ->
                    PtCard(Modifier.weight(1f)) { StatBlock(v, l, valueColor = if (l == stringResource(R.string.an_negative)) ProstutiThemeExt.colors.incorrect else MaterialTheme.colorScheme.onSurface) }
                }
            }
        }
    }
}

/** Scatter: x = seconds per question (right = slower), y = accuracy (up = better). */
@Composable
private fun SpeedAccuracyChart(a: PerformanceAnalytics) {
    val points = a.speedVsAccuracy
    val primary = MaterialTheme.colorScheme.primary
    val weak = ProstutiThemeExt.colors.incorrect
    val grid = ProstutiThemeExt.colors.chartTrack
    val maxSec = (points.maxOfOrNull { it.second.first } ?: 60f).coerceAtLeast(30f)
    val avgSec = a.avgSecondsPerQuestion
    PtCard(Modifier.fillMaxWidth()) {
        Text("↑ " + stringResource(R.string.an_more_accurate), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Canvas(Modifier.fillMaxWidth().height(160.dp).padding(vertical = 8.dp).semantics {
            contentDescription = points.joinToString { "${it.first.en}: ${it.second.first.toInt()}s, ${(it.second.second * 100).toInt()}%" }
        }) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            val xAvg = size.width * (avgSec / maxSec)
            drawLine(grid, Offset(xAvg, 0f), Offset(xAvg, size.height), 1.5.dp.toPx(), pathEffect = dash)
            drawLine(grid, Offset(0f, size.height * 0.4f), Offset(size.width, size.height * 0.4f), 1.5.dp.toPx(), pathEffect = dash)
            points.forEach { (_, p) ->
                val x = size.width * (p.first / maxSec).coerceIn(0f, 1f)
                val y = size.height * (1f - p.second.coerceIn(0f, 1f))
                val c = if (p.second < 0.6f && p.first > avgSec) weak else primary
                drawCircle(c.copy(alpha = 0.25f), 9.dp.toPx(), Offset(x, y))
                drawCircle(c, 4.dp.toPx(), Offset(x, y))
            }
        }
        Text("← " + stringResource(R.string.an_faster), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.sm))
        points.filter { it.second.second < 0.6f && it.second.first > avgSec }.forEach { (name, _) ->
            Row(verticalAlignment = Alignment.CenterVertically) { Dot(weak); Spacer(Modifier.width(6.dp)); Text(name.text(), style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun ConsistencyGrid(days: List<Int>) {
    val max = (days.maxOrNull() ?: 1).coerceAtLeast(1)
    val primary = MaterialTheme.colorScheme.primary
    val empty = ProstutiThemeExt.colors.chartTrack
    PtCard(Modifier.fillMaxWidth()) {
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { m ->
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(6.dp))
                            .background(if (m == 0) empty else primary.copy(alpha = 0.25f + 0.75f * m / max)),
                    )
                }
            }
        }
    }
}

@Suppress("unused") private val sc = @Composable { Scaffold { } }
