package app.prostuti.feature.revision

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.prostuti.core.designsystem.component.*
import app.prostuti.core.designsystem.theme.ProstutiThemeExt
import app.prostuti.core.designsystem.theme.Spacing
import app.prostuti.core.model.*
import app.prostuti.core.ui.*
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RevisionHubScreen(onQuick: (String?) -> Unit, viewModel: RevisionHubViewModel = hiltViewModel()) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    var adding by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMsg = stringResource(R.string.rv_card_saved)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { adding = true }, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text(stringResource(R.string.rv_add_card)) },
                containerColor = MaterialTheme.colorScheme.primaryContainer)
        },
    ) { padding ->
        val o = overview
        if (o == null) { SkeletonList(Modifier.padding(padding)); return@Scaffold }
        ReadableWidth {
            LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.lg, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                item { Text(stringResource(R.string.rv_title), style = MaterialTheme.typography.headlineSmall) }
                item {
                    val due = o.dueFlashcards
                    PtCard(Modifier.fillMaxWidth(), color = if (due > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer, border = due == 0) {
                        val fg = if (due > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        Text(if (due > 0) stringResource(R.string.rv_due_title, due.loc()) else stringResource(R.string.rv_nothing_due), style = MaterialTheme.typography.titleLarge, color = fg)
                        Text(stringResource(if (due > 0) R.string.rv_due_body else R.string.rv_nothing_due_body), style = MaterialTheme.typography.bodyMedium, color = fg.copy(alpha = 0.85f))
                        if (due > 0) {
                            Spacer(Modifier.height(Spacing.md))
                            Button(onClick = { nav.navigate(Destination.Flashcards()) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimary, contentColor = MaterialTheme.colorScheme.primary)) {
                                Text(stringResource(R.string.rv_review_now))
                            }
                        }
                    }
                }
                item { SectionHeader(stringResource(R.string.rv_quick)) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        QuickTile(Icons.Rounded.Functions, stringResource(R.string.rv_formulas), Modifier.weight(1f)) { onQuick(DeckKind.FORMULA.name) }
                        QuickTile(Icons.Rounded.Translate, stringResource(R.string.rv_vocabulary), Modifier.weight(1f)) { onQuick(DeckKind.VOCABULARY.name) }
                        QuickTile(Icons.Rounded.Lightbulb, stringResource(R.string.rv_facts), Modifier.weight(1f)) { onQuick(DeckKind.FACTS.name) }
                    }
                }
                item {
                    PtCard(Modifier.fillMaxWidth(), onClick = { onQuick(null) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Rounded.Bolt, tint = ProstutiThemeExt.colors.community, container = ProstutiThemeExt.colors.communityContainer)
                            Spacer(Modifier.width(Spacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.rv_last_minute), style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(R.string.rv_last_minute_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item {
                    PtCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = Spacing.xs)) {
                        ListRow(stringResource(R.string.rv_mistakes), subtitle = stringResource(R.string.rv_mistakes_meta, o.wrongAnswers.loc()),
                            leading = { IconBadge(Icons.Rounded.Replay, tint = ProstutiThemeExt.colors.incorrect, container = ProstutiThemeExt.colors.incorrectContainer, size = 36.dp) },
                            showChevron = true, onClick = { nav.navigate(Destination.WrongAnswers) })
                        HairlineDivider(inset = 64.dp)
                        ListRow(stringResource(R.string.rv_notes), subtitle = stringResource(R.string.rv_notes_meta, o.notes.loc()),
                            leading = { IconBadge(Icons.Rounded.EditNote, size = 36.dp) }, showChevron = true, onClick = { nav.navigate(Destination.Notes) })
                    }
                }
                item { SectionHeader(stringResource(R.string.rv_decks)) }
                items(o.decks, key = { it.id }) { d ->
                    PtCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Destination.Flashcards(d.id)) }, contentPadding = PaddingValues(0.dp)) {
                        ListRow(d.name.text(), subtitle = stringResource(R.string.rv_deck_meta, d.cardCount.loc(), d.dueCount.loc()),
                            leading = { IconBadge(Icons.Rounded.Style, size = 36.dp) },
                            trailing = {
                                if (d.isAvailableOffline) Pill(stringResource(R.string.rv_offline), icon = Icons.Rounded.CloudDone)
                                else IconButton(onClick = { viewModel.download(d.id) }) { Icon(Icons.Rounded.Download, stringResource(R.string.rv_download)) }
                            })
                    }
                }
            }
        }
        if (adding) AddCardSheet(o.decks, onDismiss = { adding = false }) { deck, front, back ->
            viewModel.createCard(deck, front, back) { adding = false; scope.launch { snackbar.showSnackbar(savedMsg) } }
        }
    }
}

@Composable
private fun QuickTile(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    PtCard(modifier, onClick = onClick) {
        IconBadge(icon, size = 36.dp)
        Spacer(Modifier.height(Spacing.sm))
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddCardSheet(decks: List<FlashcardDeck>, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var deck by rememberSaveable { mutableStateOf(decks.firstOrNull()?.id.orEmpty()) }
    var front by rememberSaveable { mutableStateOf("") }
    var back by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(horizontal = Spacing.gutter).padding(bottom = Spacing.xxl).navigationBarsPadding().imePadding()) {
            Text(stringResource(R.string.rv_add_card), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Spacing.md))
            Text(stringResource(R.string.rv_deck), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) { decks.forEach { d -> PtFilterChip(d.name.text(), deck == d.id, { deck = d.id }) } }
            Spacer(Modifier.height(Spacing.md))
            PtTextField(front, { front = it }, stringResource(R.string.rv_front), singleLine = false)
            Spacer(Modifier.height(Spacing.sm))
            PtTextField(back, { back = it }, stringResource(R.string.rv_back), singleLine = false, minLines = 2)
            Spacer(Modifier.height(Spacing.lg))
            PtButton(stringResource(R.string.rv_save), { onSave(deck, front.trim(), back.trim()) }, Modifier.fillMaxWidth(), enabled = deck.isNotBlank() && front.isNotBlank() && back.isNotBlank())
        }
    }
}

@Composable
internal fun FlashcardsScreen(viewModel: FlashcardsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    PtScaffold(
        stringResource(if (state.quick) R.string.rv_quick else R.string.fc_title),
        subtitle = if (!state.loading && !state.finished) stringResource(R.string.fc_progress, (state.index + 1).loc(), state.cards.size.loc()) else null,
        onBack = { nav.navigate(Destination.Back) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> SkeletonBox(Modifier.padding(Spacing.gutter).fillMaxWidth(), height = 320.dp, corner = 20.dp)
                state.cards.isEmpty() -> EmptyState(Icons.Rounded.Style, stringResource(R.string.rv_nothing_due), stringResource(R.string.fc_empty))
                state.finished -> EmptyState(
                    Icons.Rounded.TaskAlt, stringResource(R.string.fc_done_title), stringResource(R.string.fc_done_body, state.reviewed.loc()),
                    actionLabel = stringResource(R.string.fc_done), onAction = { nav.navigate(Destination.Back) },
                )
                else -> {
                    val card = state.current!!
                    ReadableWidth {
                        Column(Modifier.fillMaxSize().padding(Spacing.gutter)) {
                            MeterBar(state.index.toFloat() / state.cards.size, height = 4.dp)
                            Spacer(Modifier.height(Spacing.lg))
                            FlipCard(card, state.revealed, onFlip = viewModel::reveal, modifier = Modifier.weight(1f))
                            Spacer(Modifier.height(Spacing.lg))
                            when {
                                !state.revealed -> PtButton(stringResource(R.string.fc_show), viewModel::reveal, Modifier.fillMaxWidth())
                                state.quick -> PtButton(stringResource(R.string.fc_next), viewModel::nextQuick, Modifier.fillMaxWidth())
                                else -> GradeButtons(state.previews(Instant.now()), viewModel::grade)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlipCard(card: Flashcard, revealed: Boolean, onFlip: () -> Unit, modifier: Modifier) {
    val rotation by animateFloatAsState(if (revealed) 180f else 0f, tween(350), label = "flip")
    val showBack = rotation > 90f
    val tap = stringResource(R.string.fc_tap)
    PtCard(
        modifier.fillMaxWidth().graphicsLayer { rotationY = rotation; cameraDistance = 12f * density }
            .semantics { role = Role.Button; contentDescription = if (revealed) card.back.en else tap },
        onClick = onFlip, contentPadding = PaddingValues(Spacing.xxl),
    ) {
        Column(
            Modifier.fillMaxSize().graphicsLayer { rotationY = if (showBack) 180f else 0f },
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (card.origin == FlashcardOrigin.AI_APPROVED) Pill(stringResource(R.string.rv_origin_ai), icon = Icons.Rounded.AutoAwesome)
            Spacer(Modifier.height(Spacing.md))
            Text(
                if (showBack) card.back.text() else card.front.text(),
                style = if (showBack) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = if (showBack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            if (!showBack) {
                Spacer(Modifier.height(Spacing.lg))
                Text(tap, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun GradeButtons(previews: Map<RecallGrade, Duration>, onGrade: (RecallGrade) -> Unit) {
    val ext = ProstutiThemeExt.colors
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        listOf(
            Triple(RecallGrade.AGAIN, R.string.fc_again, ext.incorrect),
            Triple(RecallGrade.HARD, R.string.fc_hard, ext.community),
            Triple(RecallGrade.GOOD, R.string.fc_good, ext.correct),
            Triple(RecallGrade.EASY, R.string.fc_easy, ext.verified),
        ).forEach { (grade, label, color) ->
            OutlinedButton(onClick = { onGrade(grade) }, modifier = Modifier.weight(1f).heightIn(min = 64.dp), contentPadding = PaddingValues(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(label), style = MaterialTheme.typography.labelLarge, color = color)
                    previews[grade]?.let { d ->
                        val text = if (d.toHours() < 24) stringResource(R.string.fc_minutes, d.toMinutes().loc()) else stringResource(R.string.fc_days, d.toDays().loc())
                        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
internal fun NotesScreen(viewModel: NotesViewModel = hiltViewModel()) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    PtScaffold(
        stringResource(R.string.nt_title), onBack = { nav.navigate(Destination.Back) },
        floatingActionButton = { FloatingActionButton(onClick = { nav.navigate(Destination.NoteEditor()) }) { Icon(Icons.Rounded.Add, stringResource(R.string.nt_new)) } },
    ) { padding ->
        val list = notes
        when {
            list == null -> SkeletonList(Modifier.padding(padding))
            list.isEmpty() -> EmptyState(Icons.Rounded.EditNote, stringResource(R.string.nt_empty_title), stringResource(R.string.nt_empty_body), Modifier.padding(padding))
            else -> ReadableWidth {
                LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    items(list, key = { it.id }) { n ->
                        PtCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Destination.NoteEditor(n.id)) }) {
                            Text(n.title.ifBlank { n.body.take(40) }, style = MaterialTheme.typography.titleSmall)
                            Text(n.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                                Text(n.updatedAt.atZone(ZoneId.systemDefault()).toLocalDate().pretty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (!n.isSynced) { Spacer(Modifier.width(8.dp)); Pill(stringResource(R.string.nt_unsynced), icon = Icons.Rounded.CloudOff) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun NoteEditorScreen(viewModel: NoteEditorViewModel = hiltViewModel()) {
    val title by viewModel.title.collectAsStateWithLifecycle()
    val body by viewModel.body.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    val back = { nav.navigate(Destination.Back) }
    PtScaffold(
        stringResource(if (viewModel.isExisting) R.string.nt_title else R.string.nt_new), onBack = back,
        actions = {
            if (viewModel.isExisting) IconButton(onClick = { viewModel.delete(back) }) { Icon(Icons.Rounded.Delete, stringResource(R.string.nt_delete)) }
            TextButton(onClick = { viewModel.save(back) }, enabled = body.isNotBlank() || title.isNotBlank()) { Text(stringResource(R.string.nt_save)) }
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(Spacing.gutter).imePadding()) {
            PtTextField(title, { viewModel.title.value = it }, stringResource(R.string.nt_note_title))
            Spacer(Modifier.height(Spacing.md))
            PtTextField(body, { viewModel.body.value = it }, stringResource(R.string.nt_body), singleLine = false, minLines = 10)
        }
    }
}

@Suppress("unused") private val c = Modifier.clickable { }
