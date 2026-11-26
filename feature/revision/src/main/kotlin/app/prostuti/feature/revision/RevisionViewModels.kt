package app.prostuti.feature.revision

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.prostuti.core.common.AppResult
import app.prostuti.core.common.getOrNull
import app.prostuti.core.domain.repository.OfflineRepository
import app.prostuti.core.domain.repository.RevisionRepository
import app.prostuti.core.domain.revision.SpacedRepetition
import app.prostuti.core.domain.usecase.ReviewFlashcardUseCase
import app.prostuti.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class RevisionHubViewModel @Inject constructor(
    private val repo: RevisionRepository,
    private val offline: OfflineRepository,
) : ViewModel() {
    val overview = repo.overview().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init { viewModelScope.launch { repo.refresh() } }

    fun createCard(deckId: String, front: String, back: String, onDone: () -> Unit) = viewModelScope.launch {
        if (repo.createCard(deckId, front, back) is AppResult.Success) onDone()
    }

    fun download(deckId: String) = viewModelScope.launch { offline.downloadDeck(deckId) }
}

data class FlashcardSession(
    val loading: Boolean = true,
    val cards: List<Flashcard> = emptyList(),
    val index: Int = 0,
    val revealed: Boolean = false,
    val reviewed: Int = 0,
    val quick: Boolean = false,
) {
    val current: Flashcard? get() = cards.getOrNull(index)
    val finished: Boolean get() = !loading && index >= cards.size
    fun previews(now: Instant): Map<RecallGrade, Duration> = current?.let { SpacedRepetition.previewIntervals(it.srs, now) }.orEmpty()
}

@HiltViewModel
class FlashcardsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: RevisionRepository,
    private val review: ReviewFlashcardUseCase,
) : ViewModel() {
    private val deckId: String? = savedStateHandle["deckId"]
    private val quick: Boolean = savedStateHandle["quick"] ?: false
    private val kind: DeckKind? = savedStateHandle.get<String>("kind")?.let { runCatching { DeckKind.valueOf(it) }.getOrNull() }
    private val _state = MutableStateFlow(FlashcardSession(quick = quick))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val cards = if (quick) repo.quickRevision(null, kind).getOrNull().orEmpty().shuffled() else repo.dueCards(deckId, 50)
            _state.update { it.copy(loading = false, cards = cards) }
        }
    }

    fun reveal() = _state.update { it.copy(revealed = true) }

    fun grade(grade: RecallGrade) {
        val card = _state.value.current ?: return
        viewModelScope.launch { review(card, grade) }
        // "Again" puts the card back at the end of this session so it is seen once more.
        _state.update {
            val cards = if (grade == RecallGrade.AGAIN) it.cards + card else it.cards
            it.copy(cards = cards, index = it.index + 1, revealed = false, reviewed = it.reviewed + 1)
        }
    }

    fun nextQuick() = _state.update { it.copy(index = it.index + 1, revealed = false, reviewed = it.reviewed + 1) }
}

@HiltViewModel
class NotesViewModel @Inject constructor(private val repo: RevisionRepository) : ViewModel() {
    val notes = repo.notes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: RevisionRepository,
) : ViewModel() {
    private val noteId: String? = savedStateHandle["noteId"]
    private val topicId: String? = savedStateHandle["topicId"]
    val title = MutableStateFlow("")
    val body = MutableStateFlow("")
    private var existing: Note? = null

    init {
        viewModelScope.launch {
            existing = noteId?.let { id -> repo.notes().first().firstOrNull { it.id == id } }
            existing?.let { title.value = it.title; body.value = it.body }
        }
    }

    val isExisting get() = noteId != null

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val note = existing?.copy(title = title.value.trim(), body = body.value.trim())
            ?: Note("", topicId, null, title.value.trim(), body.value.trim(), Instant.now(), false)
        repo.saveNote(note)
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        noteId?.let { repo.deleteNote(it) }
        onDone()
    }
}
