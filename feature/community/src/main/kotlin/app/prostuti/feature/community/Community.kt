package app.prostuti.feature.community

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.prostuti.core.common.AppResult
import app.prostuti.core.common.getOrNull
import app.prostuti.core.designsystem.component.*
import app.prostuti.core.designsystem.theme.ProstutiThemeExt
import app.prostuti.core.designsystem.theme.Spacing
import app.prostuti.core.designsystem.theme.tabular
import app.prostuti.core.domain.repository.CommunityRepository
import app.prostuti.core.model.*
import app.prostuti.core.ui.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@Serializable data object CommunityRoute
@Serializable internal data class DiscussionRoute(val discussionId: String, val title: String)
@Serializable data class NewDiscussionRoute(val questionId: String? = null)
@Serializable internal data class LeaderboardRoute(val groupId: String, val name: String)

fun NavGraphBuilder.communityGraph(navController: NavController) {
    composable<CommunityRoute> {
        CommunityScreen(
            onOpen = { navController.navigate(DiscussionRoute(it.id, it.title)) },
            onLeaderboard = { navController.navigate(LeaderboardRoute(it.id, it.name)) },
        )
    }
    composable<DiscussionRoute> { DiscussionScreen() }
    composable<NewDiscussionRoute> { NewDiscussionScreen(onPosted = { d -> navController.navigate(DiscussionRoute(d.id, d.title)) { popUpTo<NewDiscussionRoute> { inclusive = true } } }) }
    composable<LeaderboardRoute> { LeaderboardScreen() }
}

data class CommunityState(
    val tab: Int = 0,
    val exam: String? = null,
    val discussions: UiState<List<Discussion>> = UiState.Loading,
    val groups: UiState<List<StudyGroup>> = UiState.Loading,
)

@HiltViewModel
class CommunityViewModel @Inject constructor(private val repo: CommunityRepository) : ViewModel() {
    private val _state = MutableStateFlow(CommunityState())
    val state = _state.asStateFlow()
    init { load() }

    fun load() = viewModelScope.launch {
        val d = repo.discussions(_state.value.exam)
        _state.update { it.copy(discussions = when (d) { is AppResult.Success -> UiState.Success(d.data.items); is AppResult.Failure -> UiState.Error(d.error) }) }
        _state.update { it.copy(groups = repo.groups().toUiState()) }
    }

    fun setTab(t: Int) = _state.update { it.copy(tab = t) }
    fun setExam(e: String?) { _state.update { it.copy(exam = e) }; load() }

    fun toggleMembership(g: StudyGroup) = viewModelScope.launch {
        if (g.isMember) repo.leaveGroup(g.id) else repo.joinGroup(g.id)
        _state.update { s -> s.copy(groups = (s.groups as? UiState.Success)?.let { UiState.Success(it.data.map { x -> if (x.id == g.id) x.copy(isMember = !g.isMember, memberCount = x.memberCount + if (g.isMember) -1 else 1) else x }) } ?: s.groups) }
    }
}

@HiltViewModel
class DiscussionViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val repo: CommunityRepository) : LoadViewModel<List<DiscussionReply>>() {
    val discussionId: String = checkNotNull(savedStateHandle["discussionId"])
    val title: String = savedStateHandle["title"] ?: ""
    init { load() }
    override suspend fun fetch() = repo.replies(discussionId)

    fun reply(text: String) = viewModelScope.launch {
        when (val r = repo.reply(discussionId, text)) {
            is AppResult.Success -> update { it + r.data }
            is AppResult.Failure -> report(r.error)
        }
    }

    fun reportContent(id: String, onDone: () -> Unit) = viewModelScope.launch { if (repo.report(id, "user_report") is AppResult.Success) onDone() }
}

@HiltViewModel
class NewDiscussionViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val repo: CommunityRepository) : ViewModel() {
    private val questionId: String? = savedStateHandle["questionId"]
    val hasQuestion = questionId != null
    val posting = MutableStateFlow(false)
    fun post(title: String, body: String, onDone: (Discussion) -> Unit) = viewModelScope.launch {
        posting.value = true
        repo.postDiscussion(title, body, questionId).getOrNull()?.let(onDone)
        posting.value = false
    }
}

@HiltViewModel
class LeaderboardViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val repo: CommunityRepository) : LoadViewModel<Leaderboard>() {
    private val groupId: String = checkNotNull(savedStateHandle["groupId"])
    val name: String = savedStateHandle["name"] ?: ""
    init { load() }
    override suspend fun fetch() = repo.leaderboard(groupId)
}

@Composable
private fun relative(at: Instant): String {
    val d = Duration.between(at, Instant.now())
    val raw = when {
        d.toMinutes() < 60 -> "${d.toMinutes().coerceAtLeast(1)}m"
        d.toHours() < 24 -> "${d.toHours()}h"
        else -> "${d.toDays()}d"
    }
    return raw.loc()
}

@Composable
internal fun CommunityScreen(onOpen: (Discussion) -> Unit, onLeaderboard: (StudyGroup) -> Unit, viewModel: CommunityViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    PtScaffold(
        stringResource(R.string.cm_title), onBack = { nav.navigate(Destination.Back) },
        floatingActionButton = {
            if (state.tab == 0) ExtendedFloatingActionButton(onClick = { nav.navigate(Destination.NewDiscussion()) }, icon = { Icon(Icons.Rounded.Edit, null) }, text = { Text(stringResource(R.string.cm_new)) })
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Box(Modifier.screenGutter()) {
                SegmentedControl(listOf(0, 1), state.tab, viewModel::setTab, label = { stringResource(if (it == 0) R.string.cm_discussions else R.string.cm_groups) })
            }
            if (state.tab == 0) {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.gutter, vertical = Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    PtFilterChip(stringResource(R.string.cm_all), state.exam == null, { viewModel.setExam(null) })
                    listOf("BCS", "NTRCA", "Bank SO", "Primary").forEach { e -> PtFilterChip(e, state.exam == e, { viewModel.setExam(e) }) }
                }
                StateContent(state.discussions, onRetry = { viewModel.load() }) { list ->
                    if (list.isEmpty()) EmptyState(Icons.Rounded.Forum, stringResource(R.string.cm_empty_title), stringResource(R.string.cm_empty_body))
                    else ReadableWidth {
                        LazyColumn(contentPadding = PaddingValues(start = Spacing.gutter, end = Spacing.gutter, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            items(list, key = { it.id }) { d ->
                                PtCard(Modifier.fillMaxWidth(), onClick = { onOpen(d) }) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        d.examShortName?.let { Pill(it) }
                                        if (d.questionId != null) Pill(stringResource(R.string.cm_about_question), icon = Icons.Rounded.Quiz)
                                        if (d.isResolved) Pill(stringResource(R.string.cm_resolved), color = ProstutiThemeExt.colors.correct, container = ProstutiThemeExt.colors.correctContainer, icon = Icons.Rounded.CheckCircle)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(d.title, style = MaterialTheme.typography.titleSmall)
                                    Text(d.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                    Spacer(Modifier.height(6.dp))
                                    Text("${d.authorName} · ${relative(d.createdAt)} · ${stringResource(R.string.cm_replies, d.replyCount.loc())} · ${stringResource(R.string.cm_upvotes, d.upvotes.loc())}",
                                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            } else {
                StateContent(state.groups, onRetry = { viewModel.load() }) { groups ->
                    ReadableWidth {
                        LazyColumn(contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            items(groups, key = { it.id }) { g ->
                                PtCard(Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        IconBadge(Icons.Rounded.Groups)
                                        Spacer(Modifier.width(Spacing.md))
                                        Column(Modifier.weight(1f)) {
                                            Text(g.name, style = MaterialTheme.typography.titleSmall)
                                            Text(g.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                                                Pill(stringResource(R.string.cm_members, g.memberCount.loc()))
                                                g.examShortName?.let { Pill(it) }
                                                if (g.isPrivate) Pill(stringResource(R.string.cm_private), icon = Icons.Rounded.Lock)
                                            }
                                        }
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        if (g.isMember) PtButton(stringResource(R.string.cm_leaderboard), { onLeaderboard(g) }, style = PtButtonStyle.Text, leadingIcon = Icons.Rounded.Leaderboard)
                                        PtButton(stringResource(if (g.isMember) R.string.cm_leave else R.string.cm_join), { viewModel.toggleMembership(g) }, style = if (g.isMember) PtButtonStyle.Text else PtButtonStyle.Tonal, enabled = !g.isPrivate || g.isMember)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DiscussionScreen(viewModel: DiscussionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var reply by rememberSaveable { mutableStateOf("") }
    var reporting by rememberSaveable { mutableStateOf<String?>(null) }
    val reported = stringResource(R.string.cm_reported)
    MessagesEffect(viewModel.messages, snackbar)
    PtScaffold(
        viewModel.title, onBack = { nav.navigate(Destination.Back) }, snackbarHostState = snackbar,
        actions = { IconButton(onClick = { reporting = viewModel.discussionId }) { Icon(Icons.Rounded.Flag, stringResource(R.string.cm_report)) } },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(Modifier.navigationBarsPadding().imePadding().padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                    PtTextField(reply, { reply = it }, stringResource(R.string.cm_reply_hint), singleLine = false, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(Spacing.sm))
                    FilledIconButton(onClick = { viewModel.reply(reply.trim()); reply = "" }, enabled = reply.isNotBlank(), modifier = Modifier.size(52.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.cm_send))
                    }
                }
            }
        },
    ) { padding ->
        StateContent(state, onRetry = viewModel::load, modifier = Modifier.padding(padding)) { replies ->
            ReadableWidth {
                LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    items(replies.sortedByDescending { it.isAccepted }, key = { it.id }) { r ->
                        PtCard(
                            Modifier.fillMaxWidth(),
                            color = if (r.isAccepted) ProstutiThemeExt.colors.correctContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                    Text(r.authorName.take(1), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Spacer(Modifier.width(Spacing.sm))
                                Text(r.authorName, style = MaterialTheme.typography.labelLarge)
                                if (r.isFromEditor) { Spacer(Modifier.width(6.dp)); Pill(stringResource(R.string.cm_editor), color = MaterialTheme.colorScheme.primary, container = MaterialTheme.colorScheme.primaryContainer, icon = Icons.Rounded.Verified) }
                                Spacer(Modifier.weight(1f))
                                Text(relative(r.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (r.isAccepted) Text(stringResource(R.string.cm_accepted), style = MaterialTheme.typography.labelMedium, color = ProstutiThemeExt.colors.correct, modifier = Modifier.padding(top = 6.dp))
                            Text(r.body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.cm_upvotes, r.upvotes.loc()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                TextButton(onClick = { reporting = r.id }) { Text(stringResource(R.string.cm_report), style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }
                }
            }
        }
    }
    reporting?.let { id ->
        AlertDialog(
            onDismissRequest = { reporting = null },
            title = { Text(stringResource(R.string.cm_report_title)) },
            text = { Text(stringResource(R.string.cm_report_body)) },
            confirmButton = { TextButton({ viewModel.reportContent(id) { scope.launch { snackbar.showSnackbar(reported) } }; reporting = null }) { Text(stringResource(R.string.cm_report)) } },
            dismissButton = { TextButton({ reporting = null }) { Text(stringResource(R.string.cm_cancel)) } },
        )
    }
}

@Composable
internal fun NewDiscussionScreen(onPosted: (Discussion) -> Unit, viewModel: NewDiscussionViewModel = hiltViewModel()) {
    val nav = LocalNavigator.current
    val posting by viewModel.posting.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    PtScaffold(stringResource(R.string.cm_new), onBack = { nav.navigate(Destination.Back) }) { padding ->
        Column(Modifier.padding(padding).padding(Spacing.gutter).imePadding()) {
            if (viewModel.hasQuestion) { Pill(stringResource(R.string.cm_about_question), icon = Icons.Rounded.Quiz); Spacer(Modifier.height(Spacing.md)) }
            PtTextField(title, { title = it }, stringResource(R.string.cm_title_field))
            Spacer(Modifier.height(Spacing.md))
            PtTextField(body, { body = it }, stringResource(R.string.cm_body_field), singleLine = false, minLines = 6)
            Spacer(Modifier.height(Spacing.sm))
            Text(stringResource(R.string.cm_guidelines), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Spacing.xl))
            PtButton(stringResource(R.string.cm_post), { viewModel.post(title.trim(), body.trim(), onPosted) }, Modifier.fillMaxWidth(), enabled = title.isNotBlank() && body.isNotBlank(), loading = posting)
        }
    }
}

@Composable
internal fun LeaderboardScreen(viewModel: LeaderboardViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    PtScaffold(stringResource(R.string.cm_leaderboard), subtitle = viewModel.name, onBack = { nav.navigate(Destination.Back) }) { padding ->
        StateContent(state, onRetry = viewModel::load, modifier = Modifier.padding(padding)) { lb ->
            ReadableWidth {
                LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    item { Text(lb.period, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                    items(lb.entries, key = { it.rank }) { e ->
                        PtCard(Modifier.fillMaxWidth(), color = if (e.isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainer) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(e.rank.loc(), style = MaterialTheme.typography.titleMedium.tabular, modifier = Modifier.width(32.dp),
                                    color = if (e.rank <= 3) ProstutiThemeExt.colors.streak else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (e.isMe) "${e.displayName} (${stringResource(R.string.cm_you)})" else e.displayName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                Text(stringResource(R.string.cm_points, e.score.loc()), style = MaterialTheme.typography.labelLarge.tabular)
                            }
                        }
                    }
                    item { Text(stringResource(R.string.cm_leaderboard_privacy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}
