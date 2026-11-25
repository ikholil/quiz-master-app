package app.prostuti.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.prostuti.core.designsystem.component.Pill
import app.prostuti.core.designsystem.theme.ProstutiThemeExt
import app.prostuti.core.model.Difficulty
import app.prostuti.core.model.Mastery
import app.prostuti.core.model.MasteryLevel
import app.prostuti.core.model.QuestionProvenance

/** Every question shows where it came from; the description is read out by TalkBack. */
@Composable
fun ProvenanceBadge(provenance: QuestionProvenance, modifier: Modifier = Modifier) {
    val ext = ProstutiThemeExt.colors
    val (label, desc, colors) = when (provenance) {
        QuestionProvenance.OFFICIAL -> Triple(R.string.ui_provenance_official, R.string.ui_provenance_official_desc, ext.official to MaterialTheme.colorScheme.primaryContainer)
        QuestionProvenance.VERIFIED_RECONSTRUCTED -> Triple(R.string.ui_provenance_verified, R.string.ui_provenance_verified_desc, ext.verified to ext.verifiedContainer)
        QuestionProvenance.COMMUNITY -> Triple(R.string.ui_provenance_community, R.string.ui_provenance_community_desc, ext.community to ext.communityContainer)
    }
    val icon = when (provenance) {
        QuestionProvenance.OFFICIAL -> Icons.Rounded.Verified
        QuestionProvenance.VERIFIED_RECONSTRUCTED -> Icons.Rounded.VerifiedUser
        QuestionProvenance.COMMUNITY -> Icons.Rounded.Groups
    }
    val description = stringResource(desc)
    val text = stringResource(label)
    Pill(text, modifier.semantics { contentDescription = "$text. $description" }, color = colors.first, container = colors.second, icon = icon)
}

@Composable
fun DifficultyPill(difficulty: Difficulty, modifier: Modifier = Modifier) {
    val label = when (difficulty) {
        Difficulty.EASY -> R.string.ui_difficulty_easy
        Difficulty.MEDIUM -> R.string.ui_difficulty_medium
        Difficulty.HARD -> R.string.ui_difficulty_hard
    }
    Pill(stringResource(label), modifier)
}

@Composable
fun masteryLabel(mastery: Mastery): String = stringResource(
    when (mastery.level) {
        MasteryLevel.NOT_STARTED -> R.string.ui_mastery_not_started
        MasteryLevel.WEAK -> R.string.ui_mastery_weak
        MasteryLevel.LEARNING -> R.string.ui_mastery_learning
        MasteryLevel.PROFICIENT -> R.string.ui_mastery_proficient
        MasteryLevel.MASTERED -> R.string.ui_mastery_mastered
    },
)

@Composable
fun masteryColor(mastery: Mastery): Color = when (mastery.level) {
    MasteryLevel.NOT_STARTED -> MaterialTheme.colorScheme.outline
    MasteryLevel.WEAK -> ProstutiThemeExt.colors.incorrect
    MasteryLevel.LEARNING -> ProstutiThemeExt.colors.community
    MasteryLevel.PROFICIENT -> ProstutiThemeExt.colors.verified
    MasteryLevel.MASTERED -> ProstutiThemeExt.colors.correct
}
