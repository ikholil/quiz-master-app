package app.prostuti.feature.assistant

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.prostuti.core.common.AppError
import app.prostuti.core.common.AppResult
import app.prostuti.core.common.getOrNull
import app.prostuti.core.designsystem.component.*
import app.prostuti.core.designsystem.theme.Spacing
import app.prostuti.core.domain.repository.QuestionRepository
import app.prostuti.core.domain.repository.SettingsRepository
import app.prostuti.core.domain.repository.TutorRepository
import app.prostuti.core.model.*
import app.prostuti.core.ui.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@Serializable data class AssistantRoute(val questionId: String? = null, val topicId: String? = null, val intent: String? = null)

fun NavGraphBuilder.assistantGraph(@Suppress("UNUSED_PARAMETER") navController: NavController) {
    composable<AssistantRoute> { AssistantScreen() }
}

internal val intentLabels = mapOf(
    TutorIntent.EXPLAIN_SIMPLE_BANGLA to R.string.as_intent_bangla,
    TutorIntent.EXPLAIN_ENGLISH to R.string.as_intent_english,
    TutorIntent.SOLVE_STEP_BY_STEP to R.string.as_intent_steps,
    TutorIntent.WHY_CORRECT to R.string.as_intent_why,
    TutorIntent.WHY_OTHERS_WRONG to R.string.as_intent_others,
    TutorIntent.HINT to R.string.as_intent_hint,
    TutorIntent.SUMMARIZE to R.string.as_intent_summary,
    TutorIntent.REVISION_NOTES to R.string.as_intent_notes,
    TutorIntent.FLASHCARDS to R.string.as_intent_cards,
    TutorIntent.PRACTICE_QUESTIONS to R.string.as_intent_practice,
    TutorIntent.EXPLAIN_MISTAKES to R.string.as_intent_mistakes,
    TutorIntent.FREE_CHAT to R.string.as_intent_session,
)

data class AssistantState(
    val context: TutorContext,
    val contextTitle: LocalizedText? = null,
    val messages: List<ChatMessage> = emptyList(),
    val thinking: Boolean = false,
    val usage: AiUsage? = null,
    val error: AppError? = null,
    val savedCardMessageIds: Set<String> = emptySet(),
)

@HiltViewModel
class AssistantViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tutor: TutorRepository,
    private val questions: QuestionRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val initialIntent = savedStateHandle.get<String>("intent")?.let { runCatching { TutorIntent.valueOf(it) }.getOrNull() }
    private val _state = MutableStateFlow(AssistantState(TutorContext(savedStateHandle["questionId"], savedStateHandle["topicId"])))
    val state = _state.asStateFlow()
    private var conversationId: String? = null

    /** Intents that only make sense with a question in context. */
    val availableIntents: List<TutorIntent> get() = TutorIntent.entries.filter {
        val needsQuestion = it in setOf(TutorIntent.WHY_CORRECT, TutorIntent.WHY_OTHERS_WRONG, TutorIntent.HINT, TutorIntent.SOLVE_STEP_BY_STEP)
        !needsQuestion || _state.value.context.questionId != null
    }

    init {
        viewModelScope.launch {
            _state.update { it.copy(usage = tutor.usage().getOrNull()) }
            _state.value.context.questionId?.let { id -> questions.question(id).getOrNull()?.let { q -> _state.update { it.copy(contextTitle = q.stem) } } }
            initialIntent?.let { send(it, null) }
        }
    }

    fun send(intent: TutorIntent, text: String?) {
        if (_state.value.thinking) return
        viewModelScope.launch {
            val language = settings.settings.first().language
            // Quick-intent messages are stored with a marker and rendered with the localized intent label.
            val shownText = text?.takeIf { it.isNotBlank() } ?: (INTENT_MARKER + intent.name)
            val shown = ChatMessage(UUID.randomUUID().toString(), ChatRole.STUDENT, shownText, emptyList(), Instant.now())
            _state.update { it.copy(messages = it.messages + shown, thinking = true, error = null) }
            when (val r = tutor.ask(conversationId, intent, text.orEmpty(), _state.value.context, language)) {
                is AppResult.Success -> {
                    conversationId = r.data.first
                    _state.update { it.copy(messages = it.messages + r.data.second, thinking = false, usage = it.usage?.let { u -> u.copy(usedToday = u.usedToday + 1) }) }
                }
                is AppResult.Failure -> _state.update { it.copy(thinking = false, error = r.error) }
            }
        }
    }

    companion object { const val INTENT_MARKER = "\u0000" }

    fun saveCards(message: ChatMessage) = viewModelScope.launch {
        if (tutor.saveFlashcards(message.generatedFlashcards) is AppResult.Success) {
            _state.update { it.copy(savedCardMessageIds = it.savedCardMessageIds + message.id) }
        }
    }
}

@Composable
internal fun AssistantScreen(viewModel: AssistantViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size, state.thinking) { if (state.messages.isNotEmpty()) listState.animateScrollToItem(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1) }
    PtScaffold(
        stringResource(R.string.as_title),
        subtitle = state.usage?.let { stringResource(R.string.as_remaining, it.remaining.loc(), it.dailyLimit.loc()) },
        onBack = { nav.navigate(Destination.Back) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.navigationBarsPadding().imePadding()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.md, vertical = Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        viewModel.availableIntents.filter { it != TutorIntent.FREE_CHAT }.forEach { intent ->
                            AssistChip(onClick = { viewModel.send(intent, null) }, label = { Text(stringResource(intentLabels.getValue(intent))) }, enabled = !state.thinking)
                        }
                    }
                    Row(Modifier.padding(horizontal = Spacing.md).padding(bottom = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                        PtTextField(input, { input = it }, stringResource(R.string.as_placeholder), modifier = Modifier.weight(1f), singleLine = false)
                        Spacer(Modifier.width(Spacing.sm))
                        FilledIconButton(onClick = { viewModel.send(TutorIntent.FREE_CHAT, input.trim()); input = "" }, enabled = input.isNotBlank() && !state.thinking, modifier = Modifier.size(52.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.as_send))
                        }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), state = listState, contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            item {
                state.contextTitle?.let { title ->
                    PtCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, border = false) {
                        Text(stringResource(if (state.context.questionId != null) R.string.as_context_question else R.string.as_context_topic), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(title.text(), style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                    }
                }
                if (state.messages.isEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(vertical = Spacing.xxl), horizontalAlignment = Alignment.CenterHorizontally) {
                        IconBadge(Icons.Rounded.AutoAwesome, size = 56.dp)
                        Spacer(Modifier.height(Spacing.md))
                        Text(stringResource(R.string.as_hello_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.as_hello_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(state.messages, key = { it.id }) { m ->
                if (m.role == ChatRole.STUDENT) StudentBubble(if (m.text.startsWith(AssistantViewModel.INTENT_MARKER)) stringResource(intentLabels[runCatching { TutorIntent.valueOf(m.text.drop(1)) }.getOrDefault(TutorIntent.FREE_CHAT)]!!) else m.text)
                else AssistantBubble(m, saved = m.id in state.savedCardMessageIds, onSaveCards = { viewModel.saveCards(m) }, onFollowUp = { viewModel.send(it, null) })
            }
            if (state.thinking) item { Thinking() }
            state.error?.let { e ->
                item {
                    if (e is AppError.RateLimited || e is AppError.PremiumRequired) {
                        PtCard(Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.as_limit), style = MaterialTheme.typography.bodyMedium)
                            PtButton(stringResource(R.string.as_upgrade), { nav.navigate(Destination.Subscription) }, style = PtButtonStyle.Text)
                        }
                    } else Text(errorMessage(e), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            item { Text(stringResource(R.string.as_disclaimer), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun StudentBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)).background(MaterialTheme.colorScheme.primary).padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssistantBubble(m: ChatMessage, saved: Boolean, onSaveCards: () -> Unit, onFollowUp: (TutorIntent) -> Unit) {
    Column(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
        Column(Modifier.widthIn(max = 340.dp).clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(14.dp)) {
            Text(m.text, style = MaterialTheme.typography.bodyLarge)
            if (m.generatedFlashcards.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.sm))
                m.generatedFlashcards.forEach { c ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp)) {
                        Text(c.front.text(), style = MaterialTheme.typography.labelLarge)
                        Text(c.back.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                PtButton(stringResource(if (saved) R.string.as_cards_saved else R.string.as_save_cards, m.generatedFlashcards.size.loc()), onSaveCards, style = PtButtonStyle.Text, enabled = !saved, leadingIcon = Icons.Rounded.Style)
            }
            if (m.sources.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.sm))
                SourceList(m.sources)
            }
        }
        if (m.suggestedFollowUps.isNotEmpty()) {
            FlowRow(Modifier.padding(top = Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                m.suggestedFollowUps.forEach { f -> SuggestionChip(onClick = { onFollowUp(f) }, label = { Text(stringResource(intentLabels.getValue(f))) }) }
            }
        }
    }
}

@Composable
private fun Thinking() {
    val t = rememberInfiniteTransition(label = "dots")
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        repeat(3) { i ->
            val a by t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(500, delayMillis = i * 150), RepeatMode.Reverse), label = "d$i")
            Box(Modifier.padding(end = 4.dp).size(8.dp).alpha(a).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary))
        }
        Spacer(Modifier.width(Spacing.sm))
        Text(stringResource(R.string.as_thinking), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
