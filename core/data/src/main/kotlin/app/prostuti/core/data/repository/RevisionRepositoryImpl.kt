package app.prostuti.core.data.repository

import app.prostuti.core.common.AppResult
import app.prostuti.core.common.asSuccess
import app.prostuti.core.common.map
import app.prostuti.core.data.mapper.toModel
import app.prostuti.core.data.sync.Outbox
import app.prostuti.core.data.sync.SyncOps
import app.prostuti.core.database.dao.FlashcardDao
import app.prostuti.core.database.dao.NoteDao
import app.prostuti.core.database.dao.WrongAnswerDao
import app.prostuti.core.database.entity.DeckEntity
import app.prostuti.core.database.entity.FlashcardEntity
import app.prostuti.core.database.entity.NoteEntity
import app.prostuti.core.domain.repository.RevisionRepository
import app.prostuti.core.domain.revision.SpacedRepetition
import app.prostuti.core.model.*
import app.prostuti.core.network.NetworkJson
import app.prostuti.core.network.api.RevisionApi
import app.prostuti.core.network.dto.CreateFlashcardDto
import app.prostuti.core.network.dto.FlashcardDto
import app.prostuti.core.network.dto.FlashcardReviewDto
import app.prostuti.core.network.dto.NoteDto
import app.prostuti.core.network.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

internal fun FlashcardDto.toEntity() = FlashcardEntity(
    id, deckId, front.en, front.bn, back.en, back.bn, origin, topicId, repetitions, intervalDays, easeFactor,
    Instant.parse(dueAt).toEpochMilli(), lastReviewedAt?.let { Instant.parse(it).toEpochMilli() },
)

internal fun FlashcardEntity.toModel() = Flashcard(
    id, deckId, LocalizedText(frontEn, frontBn), LocalizedText(backEn, backBn),
    runCatching { FlashcardOrigin.valueOf(origin) }.getOrDefault(FlashcardOrigin.USER), topicId,
    SrsState(repetitions, intervalDays, easeFactor, Instant.ofEpochMilli(dueAt), lastReviewedAt?.let(Instant::ofEpochMilli)),
)

internal fun Flashcard.toEntity() = FlashcardEntity(
    id, deckId, front.en, front.bn, back.en, back.bn, origin.name, topicId, srs.repetitions, srs.intervalDays,
    srs.easeFactor, srs.dueAt.toEpochMilli(), srs.lastReviewedAt?.toEpochMilli(),
)

private fun NoteEntity.toModel() = Note(id, topicId, questionId, title, body, Instant.ofEpochMilli(updatedAt), synced)

/** Offline-first: Room is the source the UI observes; reviews and edits flow to the server via the outbox. */
@Singleton
class LiveRevisionRepository @Inject constructor(
    private val api: RevisionApi,
    private val flashcards: FlashcardDao,
    private val notes: NoteDao,
    private val wrongAnswers: WrongAnswerDao,
    private val outbox: Outbox,
) : RevisionRepository {

    override fun decks(): Flow<List<FlashcardDeck>> =
        combine(flashcards.observeDecks(), flashcards.observeDeckCounts(System.currentTimeMillis())) { decks, counts ->
            val byDeck = counts.associateBy { it.deckId }
            decks.map {
                FlashcardDeck(
                    it.id, LocalizedText(it.nameEn, it.nameBn),
                    runCatching { DeckKind.valueOf(it.kind) }.getOrDefault(DeckKind.GENERAL),
                    byDeck[it.id]?.total ?: 0, byDeck[it.id]?.due ?: 0, it.offline,
                )
            }
        }

    override fun overview(): Flow<RevisionOverview> = combine(
        decks(), wrongAnswers.observeOpenCount(), notes.observeAll(),
    ) { decks, wrong, allNotes ->
        RevisionOverview(decks.sumOf { it.dueCount }, wrong, 0, allNotes.size, decks)
    }

    override suspend fun dueCards(deckId: String?, limit: Int): List<Flashcard> =
        flashcards.due(deckId, System.currentTimeMillis(), limit).map { it.toModel() }

    override suspend fun review(card: Flashcard, grade: RecallGrade): Flashcard {
        val now = Instant.now()
        val updated = card.copy(srs = SpacedRepetition.review(card.srs, grade, now))
        flashcards.upsert(updated.toEntity())
        outbox.enqueue(
            SyncOps.FLASHCARD_REVIEW,
            NetworkJson.encodeToString(FlashcardReviewDto.serializer(), FlashcardReviewDto(card.id, grade.name, now.toString())),
        )
        return updated
    }

    override suspend fun createCard(deckId: String, front: String, back: String): AppResult<Flashcard> {
        val now = Instant.now()
        val card = Flashcard(UUID.randomUUID().toString(), deckId, LocalizedText(front, front), LocalizedText(back, back), FlashcardOrigin.USER, null, SrsState.new(now))
        flashcards.upsert(card.toEntity())
        outbox.enqueue(SyncOps.FLASHCARD_CREATE, NetworkJson.encodeToString(CreateFlashcardDto.serializer(), CreateFlashcardDto(card.id, deckId, front, back)))
        return card.asSuccess()
    }

    override fun notes(): Flow<List<Note>> = notes.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun saveNote(note: Note): AppResult<Note> {
        val saved = note.copy(id = note.id.ifBlank { UUID.randomUUID().toString() }, updatedAt = Instant.now(), isSynced = false)
        notes.upsert(NoteEntity(saved.id, saved.topicId, saved.questionId, saved.title, saved.body, saved.updatedAt.toEpochMilli(), false, false))
        outbox.enqueue(
            SyncOps.NOTE_UPSERT,
            NetworkJson.encodeToString(NoteDto.serializer(), NoteDto(saved.id, saved.topicId, saved.questionId, saved.title, saved.body, saved.updatedAt.toString())),
        )
        return saved.asSuccess()
    }

    override suspend fun deleteNote(noteId: String): AppResult<Unit> {
        notes.softDelete(noteId, System.currentTimeMillis())
        outbox.enqueue(SyncOps.NOTE_DELETE, "\"$noteId\"")
        return Unit.asSuccess()
    }

    override suspend fun quickRevision(examId: String?, kind: DeckKind?) =
        safeApiCall { api.quickRevision(examId, kind?.name).data.map { it.toModel() } }

    /** Pulls decks, cards and notes. Local unsynced edits are preserved. */
    override suspend fun refresh(): AppResult<Unit> = safeApiCall {
        val decks = api.decks().data
        flashcards.upsertDecks(decks.map { DeckEntity(it.id, it.name.en, it.name.bn, it.kind, false) })
        flashcards.upsertAll(api.flashcards(null, null).data.map { it.toEntity() })
        val remoteNotes = api.notes(null).data
        remoteNotes.forEach { dto ->
            val local = notes.get(dto.id)
            if (local == null || local.synced) {
                notes.upsert(NoteEntity(dto.id, dto.topicId, dto.questionId, dto.title, dto.body, Instant.parse(dto.updatedAt).toEpochMilli(), true, false))
            }
        }
    }.map { }
}
