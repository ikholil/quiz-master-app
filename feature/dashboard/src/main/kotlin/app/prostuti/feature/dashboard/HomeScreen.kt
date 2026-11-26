package app.prostuti.feature.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.prostuti.core.designsystem.component.*
import app.prostuti.core.designsystem.theme.ProstutiThemeExt
import app.prostuti.core.designsystem.theme.Spacing
import app.prostuti.core.designsystem.theme.tabular
import app.prostuti.core.model.*
import app.prostuti.core.ui.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    MessagesEffect(viewModel.messages, snackbar)
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        StateContent(state, onRetry = viewModel::load, modifier = Modifier.padding(padding)) { d ->
            var refreshing by remember { mutableStateOf(false) }
            LaunchedEffect(state) { refreshing = false }
            PullToRefreshBox(isRefreshing = refreshing, onRefresh = { refreshing = true; viewModel.load() }, modifier = Modifier.padding(padding)) {
                ReadableWidth {
                    LazyColumn(
                        contentPadding = PaddingValues(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.md, bottom = Spacing.xxxl),
                        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                    ) {
                        item { Header(d, onNotifications = { nav.navigate(Destination.Notifications) }, onTutor = { nav.navigate(Destination.Assistant()) }) }
                        item { GoalCard(d, onClick = { nav.navigate(Destination.Planner) }) }
                        d.continueLearning?.let { c -> item { ContinueCard(c) { nav.navigate(Destination.Topic(c.topicId)) } } }
                        item {
                            SectionHeader(stringResource(R.string.home_today_plan), actionLabel = stringResource(R.string.home_full_plan), onAction = { nav.navigate(Destination.Planner) })
                        }
                        if (d.todayTasks.isEmpty()) item { Text(stringResource(R.string.home_plan_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        else item {
                            PtCard(contentPadding = PaddingValues(horizontal = Spacing.xs, vertical = Spacing.xs)) {
                                d.todayTasks.forEachIndexed { i, task ->
                                    if (i > 0) HairlineDivider(inset = 56.dp)
                                    PlanTaskRow(task, onToggle = { viewModel.toggleTask(task.id) }, onOpen = { nav.navigate(task.destination()) })
                                }
                            }
                        }
                        if (d.revisionDueCount > 0) item {
                            PtCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Destination.Revision) }, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), border = false) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconBadge(Icons.Rounded.Style, container = MaterialTheme.colorScheme.surfaceContainer)
                                    Spacer(Modifier.width(Spacing.md))
                                    Column(Modifier.weight(1f)) {
                                        Text(stringResource(R.string.home_due_title, d.revisionDueCount.loc()), style = MaterialTheme.typography.titleSmall)
                                        Text(stringResource(R.string.home_due_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        if (d.weakTopics.isNotEmpty()) {
                            item { SectionHeader(stringResource(R.string.home_weak)) }
                            item { WeakTopics(d.weakTopics) { nav.navigate(Destination.Practice(PracticeMode.TOPIC, it)) } }
                        }
                        if (d.recommendedSets.isNotEmpty()) {
                            item { SectionHeader(stringResource(R.string.home_sets)) }
                            items(d.recommendedSets, key = { it.id }) { set -> SetRow(set) { nav.navigate(Destination.Practice(PracticeMode.CUSTOM, setId = set.id)) } }
                        }
                        d.recentMock?.let { mock ->
                            item { SectionHeader(stringResource(R.string.home_recent_mock), actionLabel = stringResource(R.string.home_all_mocks), onAction = { nav.navigate(Destination.MockList()) }) }
                            item { MockCard(mock) { nav.navigate(Destination.MockHistory) } }
                        }
                        item { SectionHeader(stringResource(R.string.home_week), actionLabel = stringResource(R.string.home_analytics), onAction = { nav.navigate(Destination.Analytics) }) }
                        item { WeeklyCard(d.weekly) }
                        if (d.announcements.isNotEmpty()) {
                            item { SectionHeader(stringResource(R.string.home_announcements)) }
                            items(d.announcements, key = { it.id }) { a -> AnnouncementRow(a, context) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(d: Dashboard, onNotifications: () -> Unit, onTutor: () -> Unit) {
    val hour = LocalTime.now(ZoneId.of("Asia/Dhaka")).hour
    val greeting = stringResource(when { hour < 12 -> R.string.home_morning; hour < 17 -> R.string.home_afternoon; else -> R.string.home_evening })
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(LocalDate.now().pretty("EEEE, d MMMM"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                listOf(greeting, d.greetingName).filter { it.isNotBlank() }.joinToString(", "),
                style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        }
        IconButton(onClick = onTutor) { Icon(Icons.Rounded.AutoAwesome, stringResource(R.string.home_tutor), tint = MaterialTheme.colorScheme.primary) }
        IconButton(onClick = onNotifications) { Icon(Icons.Outlined.Notifications, stringResource(R.string.home_notifications)) }
    }
}

/** The one card that answers "how am I doing against my exam?". */
@Composable
private fun GoalCard(d: Dashboard, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    PtCard(Modifier.fillMaxWidth(), onClick = onClick, color = scheme.primary, border = false, contentPadding = PaddingValues(Spacing.xl)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(d.primaryTarget?.examName?.text().orEmpty(), style = MaterialTheme.typography.labelLarge, color = scheme.onPrimary.copy(alpha = 0.85f), maxLines = 2)
                val date = d.primaryTarget?.examDate
                if (date != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(daysUntil(date).coerceAtLeast(0).loc(), style = MaterialTheme.typography.displaySmall.tabular, color = scheme.onPrimary)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.home_days_left), style = MaterialTheme.typography.titleSmall, color = scheme.onPrimary, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Text(date.pretty(), style = MaterialTheme.typography.bodySmall, color = scheme.onPrimary.copy(alpha = 0.8f))
                } else {
                    Text(stringResource(R.string.home_no_date), style = MaterialTheme.typography.titleMedium, color = scheme.onPrimary)
                }
            }
            ProgressRing(d.syllabusCompletion, size = 76.dp, color = scheme.onPrimary, track = scheme.onPrimary.copy(alpha = 0.2f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(percent(d.syllabusCompletion), style = MaterialTheme.typography.titleSmall.tabular, color = scheme.onPrimary)
                    Text(stringResource(R.string.home_syllabus), style = MaterialTheme.typography.labelSmall, color = scheme.onPrimary.copy(alpha = 0.8f))
                }
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.home_today_target, minutesLabel(d.studiedTodayMinutes), minutesLabel(d.dailyTargetMinutes)),
                style = MaterialTheme.typography.labelMedium, color = scheme.onPrimary, modifier = Modifier.weight(1f),
            )
            Pill(stringResource(R.string.home_streak, d.streak.current.loc()), color = scheme.onPrimary, container = scheme.onPrimary.copy(alpha = 0.16f), icon = Icons.Rounded.LocalFireDepartment)
        }
        Spacer(Modifier.height(Spacing.sm))
        MeterBar(d.studiedTodayMinutes.toFloat() / d.dailyTargetMinutes.coerceAtLeast(1), color = scheme.onPrimary, track = scheme.onPrimary.copy(alpha = 0.2f))
    }
}

@Composable
private fun ContinueCard(c: ContinueLearning, onClick: () -> Unit) {
    PtCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.PlayArrow)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_continue), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(c.topicName.text(), style = MaterialTheme.typography.titleSmall)
                Text(c.subjectName.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(percent(c.progress), style = MaterialTheme.typography.labelLarge.tabular)
        }
        Spacer(Modifier.height(Spacing.md))
        MeterBar(c.progress)
    }
}

@Composable
private fun WeakTopics(topics: List<WeakTopic>, onPractice: (String) -> Unit) {
    val ext = ProstutiThemeExt.colors
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
        items(topics, key = { it.topicId }) { t ->
            PtCard(Modifier.width(240.dp), onClick = { onPractice(t.topicId) }) {
                Text(t.subjectName.text(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text(t.name.text(), style = MaterialTheme.typography.titleSmall, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(Spacing.sm))
                Text(stringResource(R.string.home_weak_accuracy, percent(t.accuracy)), style = MaterialTheme.typography.labelLarge, color = ext.incorrect)
                Text(t.reason.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, minLines = 2)
                Spacer(Modifier.height(Spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.home_practice), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(16.dp).padding(start = 2.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun SetRow(set: PracticeSetSummary, onClick: () -> Unit) {
    PtCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(set.title.text(), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.home_questions_min, set.questionCount.loc(), minutesLabel(set.estimatedMinutes)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(app.prostuti.core.ui.R.string.ui_why, set.reason.text()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (set.isDownloaded) Pill(stringResource(R.string.home_offline), icon = Icons.Rounded.CloudDone)
        }
    }
}

@Composable
private fun MockCard(m: MockAttemptHistoryItem, onClick: () -> Unit) {
    val ext = ProstutiThemeExt.colors
    PtCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Timer)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(m.mockTitle.text(), style = MaterialTheme.typography.titleSmall, maxLines = 2)
                Text(m.submittedAt.atZone(ZoneId.systemDefault()).toLocalDate().pretty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.home_mock_score, m.score.loc(), m.maxScore.loc()), style = MaterialTheme.typography.titleMedium.tabular)
                if (m.negativeMarks > 0) Text(stringResource(R.string.home_mock_negative, m.negativeMarks.loc()), style = MaterialTheme.typography.labelSmall, color = ext.incorrect)
                m.percentile?.let { Text(stringResource(R.string.home_percentile, percent(1f - it / 100f)), style = MaterialTheme.typography.labelSmall, color = ext.correct) }
            }
        }
    }
}

@Composable
private fun WeeklyCard(w: WeeklySummary) {
    val ext = ProstutiThemeExt.colors
    PtCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatBlock(minutesLabel(w.minutesStudied), stringResource(R.string.home_week_minutes, "").trim())
            StatBlock(w.questionsAnswered.loc(), stringResource(R.string.home_week_questions, "").trim())
            StatBlock(percent(w.accuracy), stringResource(R.string.home_week_accuracy, "").trim())
        }
        Spacer(Modifier.height(Spacing.lg))
        MiniBars(w.dailyMinutes.map { it.toFloat() }, Modifier.fillMaxWidth().height(56.dp), highlightIndex = w.dailyMinutes.lastIndex)
        Spacer(Modifier.height(Spacing.sm))
        val up = w.accuracyChange >= 0
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.TrendingUp, null, Modifier.size(16.dp), tint = if (up) ext.correct else ext.incorrect)
            Spacer(Modifier.width(4.dp))
            Text(
                stringResource(if (up) R.string.home_accuracy_up else R.string.home_accuracy_down, percent(kotlin.math.abs(w.accuracyChange))),
                style = MaterialTheme.typography.labelMedium, color = if (up) ext.correct else ext.incorrect,
            )
        }
    }
}

@Composable
private fun AnnouncementRow(a: Announcement, context: android.content.Context) {
    PtCard(Modifier.fillMaxWidth(), onClick = a.source?.url?.let { url -> { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) } }) {
        Row(verticalAlignment = Alignment.Top) {
            IconBadge(Icons.Rounded.Campaign, size = 36.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(a.title.text(), style = MaterialTheme.typography.titleSmall)
                Text(a.body.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    listOfNotNull(a.source?.publisher, a.publishedAt.atZone(ZoneId.systemDefault()).toLocalDate().pretty()).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
