package app.prostuti.feature.learn

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
internal fun LearnScreen(viewModel: LearnViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        StateContent(state, onRetry = viewModel::load, modifier = Modifier.padding(padding)) { o ->
            ReadableWidth {
                LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.ln_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                            PtButton(stringResource(R.string.ln_browse_exams), { nav.navigate(Destination.ExamDirectory) }, style = PtButtonStyle.Text)
                        }
                    }
                    if (o.targets.size > 1) item {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            o.targets.forEach { t -> PtFilterChip(t.examName.text(), t.examId == o.examId, { viewModel.selectExam(t.examId) }) }
                        }
                    }
                    if (o.examId == null) item {
                        EmptyState(Icons.Rounded.School, stringResource(R.string.ln_choose_exam), stringResource(R.string.ln_no_target), actionLabel = stringResource(R.string.ln_browse_exams), onAction = { nav.navigate(Destination.ExamDirectory) })
                    } else {
                        item {
                            val done = o.subjects.sumOf { it.completedTopicCount }
                            val total = o.subjects.sumOf { it.topicCount }.coerceAtLeast(1)
                            PtCard(Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.ln_overall, done.loc(), total.loc()), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    Text(percent(done.toFloat() / total), style = MaterialTheme.typography.titleSmall.tabular, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.height(Spacing.sm))
                                MeterBar(done.toFloat() / total)
                                Spacer(Modifier.height(Spacing.sm))
                                PtButton(stringResource(R.string.ln_question_bank), { nav.navigate(Destination.QuestionBank(examId = o.examId)) }, style = PtButtonStyle.Text, leadingIcon = Icons.Rounded.Quiz)
                            }
                        }
                        items(o.subjects, key = { it.id }) { s -> SubjectCard(s) { nav.navigate(Destination.Subject(s.id, o.examId)) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectCard(s: Subject, onClick: () -> Unit) {
    PtCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(if (s.topicCount == 0) 0f else s.completedTopicCount.toFloat() / s.topicCount, size = 44.dp, stroke = 5.dp) {
                Text(percent(if (s.topicCount == 0) 0f else s.completedTopicCount.toFloat() / s.topicCount), style = MaterialTheme.typography.labelSmall.tabular)
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(s.name.text(), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.ln_subject_meta, s.chapterCount.loc(), s.topicCount.loc()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(masteryColor(s.mastery), size = 6.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.ln_mastery, masteryLabel(s.mastery)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            s.marks?.let { Text(stringResource(R.string.ln_marks, it.loc()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
internal fun SubjectScreen(viewModel: SubjectViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    PtScaffold("", onBack = { nav.navigate(Destination.Back) }) { padding ->
        StateContent(state, onRetry = viewModel::load, modifier = Modifier.padding(padding)) { chapters ->
            ReadableWidth {
                LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = Spacing.xxxl)) {
                    chapters.forEach { chapter ->
                        item(key = chapter.id) {
                            Text(chapter.name.text(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.md))
                        }
                        items(chapter.topics, key = { it.id }) { t -> TopicRow(t) { nav.navigate(Destination.Topic(t.id)) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicRow(t: TopicSummary, onClick: () -> Unit) {
    val ext = ProstutiThemeExt.colors
    ListRow(
        title = t.name.text(),
        subtitle = stringResource(R.string.ln_topic_meta, minutesLabel(t.estimatedMinutes), t.questionCount.loc()) + " · " + masteryLabel(t.mastery),
        leading = {
            if (t.isCompleted) IconBadge(Icons.Rounded.CheckCircle, tint = ext.correct, container = ext.correctContainer, size = 36.dp)
            else ProgressRing(t.mastery.value, size = 36.dp, stroke = 4.dp, color = masteryColor(t.mastery))
        },
        trailing = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (t.sharedWithExamIds.size > 1) Pill(stringResource(R.string.ln_shared, t.sharedWithExamIds.size.loc()))
                if (t.isAvailableOffline) Icon(Icons.Rounded.CloudDone, stringResource(app.prostuti.core.ui.R.string.ui_offline_available), Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }
        },
        showChevron = true,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = Spacing.xs),
    )
}

@Composable
internal fun TopicScreen(viewModel: TopicViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMsg = stringResource(R.string.ln_note_saved)
    val downloadMsg = stringResource(R.string.ln_download_started)
    MessagesEffect(viewModel.messages, snackbar)
    val detail = state.dataOrNull
    PtScaffold(
        title = detail?.chapterName?.text().orEmpty(),
        onBack = { nav.navigate(Destination.Back) },
        snackbarHostState = snackbar,
        actions = {
            if (detail != null) {
                IconButton(onClick = viewModel::toggleBookmark) {
                    Icon(if (detail.isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, stringResource(R.string.ln_bookmark))
                }
                if (!detail.summary.isAvailableOffline) IconButton(onClick = { viewModel.download { scope.launch { snackbar.showSnackbar(downloadMsg) } } }) {
                    Icon(Icons.Rounded.Download, stringResource(R.string.ln_download))
                }
            }
        },
    ) { padding ->
        StateContent(state, onRetry = viewModel::load, modifier = Modifier.padding(padding)) { d ->
            ReadableWidth {
                LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    item {
                        Text(d.subjectName.text(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(d.summary.name.text(), style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(Spacing.sm))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Pill(minutesLabel(d.summary.estimatedMinutes))
                            Pill(masteryLabel(d.summary.mastery) + " · " + percent(d.summary.mastery.value), color = masteryColor(d.summary.mastery))
                            if (d.summary.isAvailableOffline) Pill(stringResource(R.string.ln_downloaded), icon = Icons.Rounded.CloudDone)
                        }
                    }
                    if (d.summary.sharedWithExamIds.size > 1) item {
                        PtCard(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), border = false) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Hub, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(Spacing.md))
                                Text(stringResource(R.string.ln_shared_desc, d.summary.sharedWithExamIds.size.loc()), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (d.prerequisites.isNotEmpty()) item {
                        SectionHeader(stringResource(R.string.ln_prereq), subtitle = stringResource(R.string.ln_prereq_desc))
                        d.prerequisites.forEach { p -> TopicRow(p) { nav.navigate(Destination.Topic(p.id)) } }
                    }
                    item { SectionHeader(stringResource(R.string.ln_materials)) }
                    items(d.materials, key = { it.id }) { m -> MaterialRow(m) { nav.navigate(Destination.Material(m.id)) } }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            PtButton(stringResource(R.string.ln_practice), { nav.navigate(Destination.Practice(PracticeMode.TOPIC, d.summary.id)) }, Modifier.fillMaxWidth(), leadingIcon = Icons.Rounded.EditNote)
                            PtButton(
                                stringResource(if (d.summary.isCompleted) R.string.ln_completed else R.string.ln_mark_complete), viewModel::toggleComplete,
                                Modifier.fillMaxWidth(), style = if (d.summary.isCompleted) PtButtonStyle.Tonal else PtButtonStyle.Outline,
                                leadingIcon = if (d.summary.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            )
                            PtButton(stringResource(R.string.ln_ask_tutor), { nav.navigate(Destination.Assistant(topicId = d.summary.id)) }, Modifier.fillMaxWidth(), style = PtButtonStyle.Text, leadingIcon = Icons.Rounded.AutoAwesome)
                        }
                    }
                    item { NoteEditor(d.note.orEmpty()) { text -> viewModel.saveNote(text) { scope.launch { snackbar.showSnackbar(savedMsg) } } } }
                }
            }
        }
    }
}

@Composable
private fun MaterialRow(m: LearningMaterialSummary, onClick: () -> Unit) {
    val (icon, label) = when (m.type) {
        MaterialType.ARTICLE -> Icons.AutoMirrored.Rounded.Article to R.string.ln_type_article
        MaterialType.AUDIO -> Icons.Rounded.Headphones to R.string.ln_type_audio
        MaterialType.VIDEO -> Icons.Rounded.PlayCircle to R.string.ln_type_video
        MaterialType.PDF -> Icons.Rounded.PictureAsPdf to R.string.ln_type_pdf
    }
    PtCard(Modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(0.dp)) {
        ListRow(
            m.title.text(),
            subtitle = listOfNotNull(stringResource(label), m.durationMinutes?.let { minutesLabel(it) }).joinToString(" · "),
            leading = { IconBadge(icon, size = 36.dp) },
            trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (m.isPremium) Pill(stringResource(R.string.ln_premium), color = ProstutiThemeExt.colors.community, container = ProstutiThemeExt.colors.communityContainer)
                    if (m.isDownloaded) Icon(Icons.Rounded.CloudDone, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
            },
            showChevron = true,
        )
    }
}

@Composable
private fun NoteEditor(initial: String, onSave: (String) -> Unit) {
    var text by rememberSaveable(initial) { mutableStateOf(initial) }
    Column {
        SectionHeader(stringResource(R.string.ln_note))
        Spacer(Modifier.height(Spacing.sm))
        PtTextField(text, { text = it }, stringResource(R.string.ln_note), placeholder = stringResource(R.string.ln_note_hint), singleLine = false, minLines = 3)
        if (text != initial) PtButton(stringResource(R.string.ln_note_save), { onSave(text) }, style = PtButtonStyle.Text)
    }
}

@Composable
internal fun MaterialScreen(viewModel: MaterialViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    val m = state.dataOrNull
    PtScaffold(
        m?.summary?.title?.text().orEmpty(),
        onBack = { nav.navigate(Destination.Back) },
        actions = {
            if (m != null) IconButton(onClick = { nav.navigate(Destination.Assistant(topicId = m.summary.topicId, intent = TutorIntent.SUMMARIZE)) }) {
                Icon(Icons.Rounded.AutoAwesome, stringResource(R.string.ln_summarize))
            }
        },
    ) { padding ->
        StateContent(state, onRetry = viewModel::load, modifier = Modifier.padding(padding)) { mat ->
            ReadableWidth {
                LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(Spacing.gutter)) {
                    item { ContentBlocks(mat.blocks) }
                    item {
                        Spacer(Modifier.height(Spacing.xl))
                        SourceList(mat.sources)
                        Text(
                            stringResource(R.string.ln_updated, mat.updatedAt.atZone(ZoneId.systemDefault()).toLocalDate().pretty()),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
