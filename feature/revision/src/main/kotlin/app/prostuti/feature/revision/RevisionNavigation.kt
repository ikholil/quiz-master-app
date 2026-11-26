package app.prostuti.feature.revision

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object RevisionRoute
/** [quick] = rapid flip-through without scheduling; [kind] limits quick revision to a deck kind. */
@Serializable data class FlashcardsRoute(val deckId: String? = null, val quick: Boolean = false, val kind: String? = null)
@Serializable data object NotesRoute
@Serializable data class NoteEditorRoute(val noteId: String? = null, val topicId: String? = null)

fun NavGraphBuilder.revisionGraph(navController: NavController) {
    composable<RevisionRoute> { RevisionHubScreen(onQuick = { kind -> navController.navigate(FlashcardsRoute(quick = true, kind = kind)) }) }
    composable<FlashcardsRoute> { FlashcardsScreen() }
    composable<NotesRoute> { NotesScreen() }
    composable<NoteEditorRoute> { NoteEditorScreen() }
}
