package app.prostuti.feature.exams

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.launch
import java.time.ZoneId

@Composable
internal fun categoryLabel(c: ExamCategory) = stringResource(
    when (c) {
        ExamCategory.BCS -> R.string.ex_cat_bcs
        ExamCategory.GOVERNMENT_JOB -> R.string.ex_cat_gov
        ExamCategory.BANK -> R.string.ex_cat_bank
        ExamCategory.NTRCA -> R.string.ex_cat_ntrca
        ExamCategory.PRIMARY_TEACHER -> R.string.ex_cat_primary
        ExamCategory.UNIVERSITY_ADMISSION -> R.string.ex_cat_admission
        ExamCategory.OTHER -> R.string.ex_cat_other
    },
)

@Composable
internal fun ExamDirectoryScreen(onCompare: (List<String>) -> Unit, viewModel: ExamDirectoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    PtScaffold(
        title = stringResource(R.string.ex_title),
        onBack = { nav.navigate(Destination.Back) },
        actions = {
            IconButton(onClick = viewModel::toggleCompareMode) {
                Icon(Icons.Rounded.CompareArrows, stringResource(R.string.ex_compare), tint = if (state.compareMode) MaterialTheme.colorScheme.primary else LocalContentColor.current)
            }
        },
        bottomBar = {
            if (state.compareMode) Surface(tonalElevation = 0.dp, color = MaterialTheme.colorScheme.surfaceContainer) {
                PtButton(
                    if (state.compareIds.size >= 2) stringResource(R.string.ex_compare_selected, state.compareIds.size.loc()) else stringResource(R.string.ex_compare_hint),
                    { onCompare(state.compareIds) }, Modifier.fillMaxWidth().navigationBarsPadding().padding(Spacing.lg), enabled = state.compareIds.size >= 2,
                )
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            PtTextField(
                state.filter.query, viewModel::onQuery, stringResource(R.string.ex_search),
                leadingIcon = Icons.Rounded.Search, modifier = Modifier.screenGutter(),
            )
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.gutter, vertical = Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PtFilterChip(stringResource(R.string.ex_all), state.filter.category == null && !state.filter.bookmarkedOnly, { viewModel.onCategory(null) })
                PtFilterChip(stringResource(R.string.ex_saved), state.filter.bookmarkedOnly, viewModel::onSavedOnly, icon = Icons.Rounded.Bookmark)
                ExamCategory.entries.filter { it != ExamCategory.OTHER }.forEach { c ->
                    PtFilterChip(categoryLabel(c), state.filter.category == c, { viewModel.onCategory(c) })
                }
            }
            val listState = rememberLazyListState()
            val nearEnd by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let { it >= listState.layoutInfo.totalItemsCount - 3 } == true } }
            LaunchedEffect(nearEnd) { if (nearEnd) viewModel.loadMore() }
            StateContent(state.exams, onRetry = { viewModel.search(false) }) { exams ->
                if (exams.isEmpty()) EmptyState(Icons.Rounded.SearchOff, stringResource(R.string.ex_empty_title), stringResource(R.string.ex_empty_body))
                else ReadableWidth {
                    LazyColumn(state = listState, contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        items(exams, key = { it.id }) { exam ->
                            ExamCard(
                                exam,
                                compareMode = state.compareMode,
                                comparing = exam.id in state.compareIds,
                                onClick = { if (state.compareMode) viewModel.toggleCompare(exam.id) else nav.navigate(Destination.Exam(exam.id)) },
                                onBookmark = { viewModel.toggleBookmark(exam) },
                            )
                        }
                        if (state.loadingMore) item { SkeletonBox(Modifier.fillMaxWidth(), height = 96.dp, corner = 14.dp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamCard(exam: ExamSummary, compareMode: Boolean, comparing: Boolean, onClick: () -> Unit, onBookmark: () -> Unit) {
    PtCard(Modifier.fillMaxWidth(), onClick = onClick, color = if (comparing) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainer) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(categoryLabel(exam.category))
                    if (exam.isTarget) Pill(stringResource(R.string.ex_target), color = MaterialTheme.colorScheme.primary, container = MaterialTheme.colorScheme.primaryContainer)
                }
                Spacer(Modifier.height(6.dp))
                Text(exam.name.text(), style = MaterialTheme.typography.titleSmall)
                Text(exam.authority.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (compareMode) Checkbox(comparing, { onClick() })
            else IconButton(onClick = onBookmark) {
                Icon(
                    if (exam.isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    stringResource(if (exam.isBookmarked) R.string.ex_unbookmark else R.string.ex_bookmark),
                    tint = if (exam.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        exam.nextDeadline?.let { d ->
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Event, null, Modifier.size(16.dp), tint = ProstutiThemeExt.colors.community)
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.ex_deadline, d.label.text(), d.date.pretty()) + " · " + stringResource(app.prostuti.core.ui.R.string.ui_days_left, daysUntil(d.date).loc()),
                    style = MaterialTheme.typography.labelMedium, color = ProstutiThemeExt.colors.community,
                )
            }
        }
    }
}

@Composable
internal fun ExamDetailScreen(viewModel: ExamDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val addedMsg = stringResource(R.string.ex_target_added)
    MessagesEffect(viewModel.messages, snackbar)
    val detail = state.dataOrNull
    PtScaffold(
        title = detail?.summary?.shortName.orEmpty(),
        onBack = { nav.navigate(Destination.Back) },
        snackbarHostState = snackbar,
        actions = {
            if (detail != null) IconButton(onClick = viewModel::toggleBookmark) {
                Icon(if (detail.summary.isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, stringResource(R.string.ex_bookmark))
            }
        },
    ) { padding ->
        StateContent(state, onRetry = viewModel::load, modifier = Modifier.padding(padding)) { d ->
            ReadableWidth {
                LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    item {
                        Text(d.summary.name.text(), style = MaterialTheme.typography.headlineSmall)
                        Text(d.summary.authority.text(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(Spacing.sm))
                        Text(d.description.text(), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(Spacing.lg))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            PtButton(stringResource(R.string.ex_open_syllabus), { nav.navigate(Destination.Syllabus(d.summary.id)) }, Modifier.weight(1f))
                            PtButton(stringResource(R.string.ex_mock_tests), { nav.navigate(Destination.MockList(d.summary.id)) }, Modifier.weight(1f), style = PtButtonStyle.Outline)
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        if (d.summary.isTarget) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = ProstutiThemeExt.colors.correct, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.ex_is_target), style = MaterialTheme.typography.labelLarge, color = ProstutiThemeExt.colors.correct)
                            }
                        } else PtButton(stringResource(R.string.ex_add_target), { viewModel.addTarget { scope.launch { snackbar.showSnackbar(addedMsg) } } }, style = PtButtonStyle.Text)
                    }
                    item { SectionHeader(stringResource(R.string.ex_stages), subtitle = stringResource(R.string.ex_previous_papers, d.previousPaperCount.loc())) }
                    items(d.stages, key = { it.id }) { StageCard(it) }
                    if (d.deadlines.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.ex_dates)) }
                        item {
                            PtCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = Spacing.xs)) {
                                d.deadlines.sortedBy { it.date }.forEach { dl ->
                                    ListRow(dl.label.text(), subtitle = dl.date.pretty(), leading = { IconBadge(Icons.Rounded.Event, size = 36.dp) },
                                        trailing = { Text(stringResource(app.prostuti.core.ui.R.string.ui_days_left, daysUntil(dl.date).loc()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) })
                                }
                            }
                        }
                    }
                    item { SectionHeader(stringResource(R.string.ex_eligibility)) }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            d.eligibility.forEach { Row { Text("•  ", color = MaterialTheme.colorScheme.primary); Text(it.text(), style = MaterialTheme.typography.bodyMedium) } }
                        }
                    }
                    item { SectionHeader(stringResource(R.string.ex_notices)) }
                    item { SourceList(d.notices.map { it.source.copy(title = it.title.text()) }) }
                    item {
                        Text(
                            stringResource(R.string.ex_source, d.officialSource.publisher, d.lastVerifiedAt.atZone(ZoneId.systemDefault()).toLocalDate().pretty()),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StageCard(stage: ExamStage) {
    val ext = ProstutiThemeExt.colors
    PtCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stage.name.text(), style = MaterialTheme.typography.titleSmall)
                Text(stage.format.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(stringResource(R.string.ex_marks, stage.totalMarks.loc()), style = MaterialTheme.typography.titleSmall.tabular)
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            stage.durationMinutes?.let { Pill(minutesLabel(it)) }
            if (stage.negativeMarkPerWrong > 0) Pill(stringResource(R.string.ex_negative, stage.negativeMarkPerWrong.loc()), color = ext.incorrect, container = ext.incorrectContainer)
            else Pill(stringResource(R.string.ex_no_negative))
            stage.passMarks?.let { Pill(stringResource(R.string.ex_pass, it.loc())) }
        }
        if (stage.subjects.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.md))
            val max = stage.subjects.maxOf { it.marks }
            stage.subjects.forEach { s ->
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.subjectName.text(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(Modifier.width(90.dp).padding(horizontal = 8.dp)) { MeterBar((s.marks / max).toFloat(), height = 5.dp) }
                    Text(s.marks.loc(), style = MaterialTheme.typography.labelMedium.tabular, modifier = Modifier.width(32.dp))
                }
            }
        }
    }
}

@Composable
internal fun CompareScreen(viewModel: CompareViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    PtScaffold(stringResource(R.string.ex_compare_title), onBack = { nav.navigate(Destination.Back) }) { padding ->
        StateContent(state, onRetry = viewModel::load, modifier = Modifier.padding(padding)) { c ->
            LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                item {
                    PtCard(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), border = false) {
                        Text(stringResource(R.string.ex_shared_topics, c.sharedTopicCount.loc()), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                item {
                    PtCard(contentPadding = PaddingValues(0.dp)) {
                        Row(Modifier.padding(Spacing.md)) {
                            Text(stringResource(R.string.ex_subject), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1.6f))
                            c.exams.forEach { Text(it.shortName, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f)) }
                        }
                        c.rows.forEach { row ->
                            HairlineDivider()
                            Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                                Text(row.subjectName.text(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.6f))
                                c.exams.forEach { e ->
                                    val marks = row.marksByExam[e.id]
                                    Text(marks?.loc() ?: stringResource(R.string.ex_not_included), style = MaterialTheme.typography.bodyMedium.tabular,
                                        color = if (marks == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("unused") private val lr = @Composable { LazyRow { } }
