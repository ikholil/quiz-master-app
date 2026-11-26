package app.prostuti.feature.questionbank

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.prostuti.core.designsystem.component.*
import app.prostuti.core.designsystem.theme.ProstutiThemeExt
import app.prostuti.core.designsystem.theme.Spacing
import app.prostuti.core.model.*
import app.prostuti.core.ui.*
import kotlinx.coroutines.launch

@Composable
internal fun QuestionBankScreen(viewModel: QuestionBankViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    val f = state.filter
    PtScaffold(stringResource(R.string.qb_title), onBack = { nav.navigate(Destination.Back) }) { padding ->
        Column(Modifier.padding(padding)) {
            PtTextField(f.query, { q -> viewModel.setFilter({ it.copy(query = q) }, debounce = true) }, stringResource(R.string.qb_search), leadingIcon = Icons.Rounded.Search, modifier = Modifier.screenGutter())
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.gutter, vertical = Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PtFilterChip(stringResource(R.string.qb_previous_year), f.previousYearOnly, { viewModel.setFilter({ it.copy(previousYearOnly = !it.previousYearOnly) }) }, icon = Icons.Rounded.History)
                PtFilterChip(stringResource(R.string.qb_saved), f.bookmarkedOnly, { viewModel.setFilter({ it.copy(bookmarkedOnly = !it.bookmarkedOnly) }) }, icon = Icons.Rounded.Bookmark)
                listOf(QuestionProvenance.OFFICIAL to R.string.qb_official, QuestionProvenance.VERIFIED_RECONSTRUCTED to R.string.qb_verified, QuestionProvenance.COMMUNITY to R.string.qb_community).forEach { (p, l) ->
                    PtFilterChip(stringResource(l), f.provenance == p, { viewModel.setFilter({ it.copy(provenance = if (it.provenance == p) null else p) }) })
                }
                listOf(Difficulty.EASY to R.string.qb_easy, Difficulty.HARD to R.string.qb_hard).forEach { (d, l) ->
                    PtFilterChip(stringResource(l), f.difficulty == d, { viewModel.setFilter({ it.copy(difficulty = if (it.difficulty == d) null else d) }) })
                }
                PtFilterChip(stringResource(R.string.qb_written), f.type == QuestionType.WRITTEN, { viewModel.setFilter({ it.copy(type = if (it.type == QuestionType.WRITTEN) null else QuestionType.WRITTEN) }) })
            }
            val listState = rememberLazyListState()
            val nearEnd by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let { it >= listState.layoutInfo.totalItemsCount - 2 } == true } }
            LaunchedEffect(nearEnd) { if (nearEnd) viewModel.loadMore() }
            StateContent(state.questions, onRetry = { viewModel.refresh() }) { questions ->
                if (questions.isEmpty()) EmptyState(Icons.Rounded.SearchOff, stringResource(R.string.qb_empty_title), stringResource(R.string.qb_empty_body))
                else ReadableWidth {
                    LazyColumn(state = listState, contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        items(questions, key = { it.id }) { q ->
                            val revealed = q.id in state.revealed
                            PtCard(Modifier.fillMaxWidth()) {
                                QuestionView(q, state.selections[q.id].orEmpty(), onSelect = { viewModel.select(q, it) }, revealed = revealed)
                                Spacer(Modifier.height(Spacing.sm))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PtButton(stringResource(if (revealed) R.string.qb_hide_answer else R.string.qb_show_answer), { viewModel.toggleReveal(q.id) }, style = PtButtonStyle.Text)
                                    Spacer(Modifier.weight(1f))
                                    PtButton(stringResource(R.string.qb_question), { nav.navigate(Destination.Question(q.id)) }, style = PtButtonStyle.Text, trailingIcon = Icons.Rounded.ChevronRight)
                                }
                                AnimatedVisibility(revealed) { SolutionView(q) }
                            }
                        }
                        if (state.loadingMore) item { SkeletonBox(Modifier.fillMaxWidth(), height = 160.dp, corner = 14.dp) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuestionDetailScreen(viewModel: QuestionDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val revealed by viewModel.revealed.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var reporting by rememberSaveable { mutableStateOf(false) }
    var collecting by rememberSaveable { mutableStateOf(false) }
    val thanks = stringResource(R.string.qb_report_thanks)
    val addedFmt = stringResource(R.string.qb_added, "%s")
    MessagesEffect(viewModel.messages, snackbar)
    val detail = state.dataOrNull
    PtScaffold(
        title = detail?.question?.topicName?.text().orEmpty(),
        subtitle = detail?.question?.subjectName?.text(),
        onBack = { nav.navigate(Destination.Back) },
        snackbarHostState = snackbar,
        actions = {
            if (detail != null) {
                IconButton(onClick = viewModel::toggleBookmark) {
                    Icon(if (detail.question.isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        stringResource(if (detail.question.isBookmarked) R.string.qb_unbookmark else R.string.qb_bookmark))
                }
                IconButton(onClick = { collecting = true }) { Icon(Icons.Rounded.PlaylistAdd, stringResource(R.string.qb_add_collection)) }
                IconButton(onClick = { reporting = true }) { Icon(Icons.Rounded.Flag, stringResource(R.string.qb_report)) }
            }
        },
    ) { padding ->
        StateContent(state, onRetry = viewModel::load, modifier = Modifier.padding(padding)) { d ->
            val q = d.question
            ReadableWidth {
                LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    item { QuestionView(q, selection, onSelect = viewModel::select, revealed = revealed) }
                    item {
                        if (!revealed) Text(stringResource(R.string.qb_try_first), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            PtButton(stringResource(if (revealed) R.string.qb_hide_answer else R.string.qb_show_answer), viewModel::reveal, style = PtButtonStyle.Tonal, modifier = Modifier.weight(1f))
                            if (!revealed) PtButton(stringResource(R.string.qb_hint), { nav.navigate(Destination.Assistant(questionId = q.id, intent = TutorIntent.HINT)) }, style = PtButtonStyle.Outline, modifier = Modifier.weight(1f), leadingIcon = Icons.Rounded.Lightbulb)
                        }
                    }
                    if (revealed) {
                        item { SolutionView(q) }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                PtButton(stringResource(R.string.qb_ask_tutor), { nav.navigate(Destination.Assistant(questionId = q.id, intent = TutorIntent.WHY_CORRECT)) }, Modifier.weight(1f), style = PtButtonStyle.Outline, leadingIcon = Icons.Rounded.AutoAwesome)
                                PtButton(stringResource(R.string.qb_discuss), { nav.navigate(Destination.NewDiscussion(q.id)) }, Modifier.weight(1f), style = PtButtonStyle.Outline, leadingIcon = Icons.AutoMirrored.Rounded.Chat)
                            }
                        }
                    }
                    item { QuestionNote(q.note.orEmpty(), viewModel::saveNote) }
                    if (d.similar.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.qb_similar)) }
                        items(d.similar, key = { it.id }) { s ->
                            PtCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Destination.Question(s.id)) }) {
                                Text(s.stem.text(), style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { ProvenanceBadge(s.provenance); DifficultyPill(s.difficulty) }
                            }
                        }
                    }
                }
            }
            if (reporting) ReportSheet(onDismiss = { reporting = false }) { reason, details ->
                viewModel.report(reason, details) { reporting = false; scope.launch { snackbar.showSnackbar(thanks) } }
            }
            if (collecting) CollectionSheet(d.collections, onDismiss = { collecting = false },
                onPick = { c -> viewModel.addToCollection(c) { name -> collecting = false; scope.launch { snackbar.showSnackbar(addedFmt.replace("%s", name)) } } },
                onCreate = { name -> viewModel.createCollection(name) { n -> collecting = false; scope.launch { snackbar.showSnackbar(addedFmt.replace("%s", n)) } } })
        }
    }
}

@Composable
private fun QuestionNote(initial: String, onSave: (String) -> Unit) {
    var text by rememberSaveable(initial) { mutableStateOf(initial) }
    Column {
        PtTextField(text, { text = it }, stringResource(R.string.qb_note), placeholder = stringResource(R.string.qb_note_hint), singleLine = false, minLines = 2, leadingIcon = Icons.Rounded.EditNote)
        if (text != initial) PtButton(stringResource(R.string.qb_save), { onSave(text) }, style = PtButtonStyle.Text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportSheet(onDismiss: () -> Unit, onSend: (QuestionReportReason, String) -> Unit) {
    var reason by rememberSaveable { mutableStateOf(QuestionReportReason.WRONG_ANSWER) }
    var details by rememberSaveable { mutableStateOf("") }
    val labels = mapOf(
        QuestionReportReason.WRONG_ANSWER to R.string.qb_report_wrong, QuestionReportReason.TYPO to R.string.qb_report_typo,
        QuestionReportReason.UNCLEAR to R.string.qb_report_unclear, QuestionReportReason.OUTDATED to R.string.qb_report_outdated,
        QuestionReportReason.DUPLICATE to R.string.qb_report_duplicate, QuestionReportReason.OTHER to R.string.qb_report_other,
    )
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(horizontal = Spacing.gutter).padding(bottom = Spacing.xxl).navigationBarsPadding()) {
            Text(stringResource(R.string.qb_report_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.sm))
            labels.forEach { (r, l) ->
                Row(Modifier.fillMaxWidth().selectable(reason == r, role = Role.RadioButton) { reason = r }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(reason == r, null)
                    Spacer(Modifier.width(Spacing.md))
                    Text(stringResource(l), style = MaterialTheme.typography.bodyLarge)
                }
            }
            PtTextField(details, { details = it }, stringResource(R.string.qb_report_details), singleLine = false, minLines = 2)
            Spacer(Modifier.height(Spacing.lg))
            PtButton(stringResource(R.string.qb_report_send), { onSend(reason, details) }, Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionSheet(collections: List<QuestionCollection>, onDismiss: () -> Unit, onPick: (QuestionCollection) -> Unit, onCreate: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(horizontal = Spacing.gutter).padding(bottom = Spacing.xxl).navigationBarsPadding()) {
            Text(stringResource(R.string.qb_add_collection), style = MaterialTheme.typography.titleMedium)
            collections.forEach { c ->
                ListRow(c.name, subtitle = c.questionCount.loc(), leading = { IconBadge(Icons.Rounded.Folder, size = 36.dp) }, onClick = { onPick(c) })
            }
            Spacer(Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PtTextField(name, { name = it }, stringResource(R.string.qb_new_collection), modifier = Modifier.weight(1f))
                Spacer(Modifier.width(Spacing.sm))
                PtButton(stringResource(R.string.qb_create), { onCreate(name.trim()) }, enabled = name.isNotBlank(), style = PtButtonStyle.Tonal)
            }
        }
    }
}

@Composable
internal fun WrongAnswersScreen(viewModel: WrongAnswersViewModel = hiltViewModel()) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val showResolved by viewModel.showResolved.collectAsStateWithLifecycle()
    val expanded by viewModel.expanded.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    val ext = ProstutiThemeExt.colors
    PtScaffold(stringResource(R.string.wa_title), onBack = { nav.navigate(Destination.Back) }) { padding ->
        val list = entries
        if (list == null) { SkeletonList(Modifier.padding(padding)); return@PtScaffold }
        val visible = list.filter { it.resolved == showResolved }
        ReadableWidth {
            LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                item {
                    Text(stringResource(R.string.wa_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(Spacing.md))
                    SegmentedControl(listOf(false, true), showResolved, { viewModel.showResolved.value = it },
                        label = { stringResource(if (it) R.string.wa_resolved else R.string.wa_open) + " (" + list.count { e -> e.resolved == it }.loc() + ")" })
                    if (!showResolved && visible.isNotEmpty()) {
                        Spacer(Modifier.height(Spacing.md))
                        PtButton(stringResource(R.string.wa_practice), { nav.navigate(Destination.Practice(PracticeMode.WRONG_ANSWERS)) }, Modifier.fillMaxWidth(), leadingIcon = Icons.Rounded.Replay)
                    }
                }
                if (visible.isEmpty()) item { EmptyState(Icons.Rounded.TaskAlt, stringResource(R.string.wa_empty_title), stringResource(R.string.wa_empty_body)) }
                items(visible, key = { it.question.id }) { e ->
                    val open = e.question.id in expanded
                    PtCard(Modifier.fillMaxWidth(), onClick = { viewModel.toggle(e.question.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Pill(stringResource(R.string.wa_times, e.timesWrong.loc()), color = ext.incorrect, container = ext.incorrectContainer)
                            Spacer(Modifier.width(6.dp))
                            Text(e.question.topicName.text(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), maxLines = 1)
                            Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
                        }
                        Spacer(Modifier.height(6.dp))
                        if (open) {
                            QuestionView(e.question, e.selectedOptionIds, onSelect = null, revealed = true, showMeta = false)
                            Spacer(Modifier.height(Spacing.md))
                            SolutionView(e.question)
                            if (!e.resolved) PtButton(stringResource(R.string.wa_mark_resolved), { viewModel.resolve(e.question.id) }, style = PtButtonStyle.Tonal, leadingIcon = Icons.Rounded.Check)
                        } else {
                            Text(e.question.stem.text(), style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                            val lang = LocalAppLanguage.current
                            val chosen = e.question.options.filter { it.id in e.selectedOptionIds }.joinToString { it.text.resolve(lang) }
                            if (chosen.isNotBlank()) Text(stringResource(R.string.wa_you_chose, chosen), style = MaterialTheme.typography.bodySmall, color = ext.incorrect)
                        }
                    }
                }
            }
        }
    }
}
