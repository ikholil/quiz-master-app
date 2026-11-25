package app.prostuti.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.prostuti.core.designsystem.component.Pill
import app.prostuti.core.designsystem.theme.ProstutiThemeExt
import app.prostuti.core.designsystem.theme.Spacing
import app.prostuti.core.model.Question
import app.prostuti.core.model.QuestionType
import coil.compose.AsyncImage

/** How an option should look. Answer colors are only used after reveal. */
enum class OptionState { Neutral, Selected, Correct, Incorrect, MissedCorrect, Dimmed }

private val OPTION_LETTERS_EN = listOf("A", "B", "C", "D", "E", "F")
private val OPTION_LETTERS_BN = listOf("ক", "খ", "গ", "ঘ", "ঙ", "চ")

/**
 * Renders a question: meta row, optional passage, stem, image, formula and options.
 * [revealed] switches options into answer-review mode using the solution's correct ids.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestionView(
    question: Question,
    selected: Set<String>,
    onSelect: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
    revealed: Boolean = false,
    number: Int? = null,
    showMeta: Boolean = true,
) {
    val lang = LocalAppLanguage.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        if (showMeta) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ProvenanceBadge(question.provenance)
                DifficultyPill(question.difficulty)
                question.examReferences.firstOrNull()?.let { ref ->
                    Pill(listOfNotNull(ref.examName.text(), ref.year?.toString()).joinToString(" · ").loc())
                }
            }
        }
        question.passage?.let { PassageCard(it.title?.text(), it.text.text()) }
        Text(
            text = (number?.let { "${it.loc()}. " } ?: "") + question.stem.text(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
        )
        question.imageUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = question.stem.text(),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
        question.formula?.let { FormulaBox(it) }
        if (question.type == QuestionType.MULTI_SELECT && !revealed) {
            Text(stringResource(R.string.ui_multi_select_hint), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        if (question.type in setOf(QuestionType.WRITTEN, QuestionType.SHORT_ANSWER, QuestionType.VIVA) && question.options.isEmpty()) {
            Text(stringResource(R.string.ui_written_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val correct = question.solution?.correctOptionIds.orEmpty()
        val letters = if (lang == app.prostuti.core.model.AppLanguage.BENGALI) OPTION_LETTERS_BN else OPTION_LETTERS_EN
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            question.options.forEachIndexed { index, option ->
                val isSelected = option.id in selected
                val state = when {
                    !revealed -> if (isSelected) OptionState.Selected else OptionState.Neutral
                    option.id in correct && isSelected -> OptionState.Correct
                    option.id in correct -> OptionState.MissedCorrect
                    isSelected -> OptionState.Incorrect
                    else -> OptionState.Dimmed
                }
                OptionRow(
                    letter = letters.getOrElse(index) { "${index + 1}" },
                    text = option.text.text(),
                    imageUrl = option.imageUrl,
                    state = state,
                    multiSelect = question.type == QuestionType.MULTI_SELECT,
                    onClick = onSelect?.let { { it(option.id) } },
                )
            }
        }
    }
}

@Composable
fun OptionRow(
    letter: String,
    text: String,
    state: OptionState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    multiSelect: Boolean = false,
) {
    val ext = ProstutiThemeExt.colors
    val scheme = MaterialTheme.colorScheme
    val (borderColor, bg, badgeBg, badgeFg) = when (state) {
        OptionState.Neutral -> Quad(scheme.outlineVariant, scheme.surfaceContainer, scheme.surfaceVariant, scheme.onSurfaceVariant)
        OptionState.Selected -> Quad(scheme.primary, scheme.primaryContainer.copy(alpha = 0.5f), scheme.primary, scheme.onPrimary)
        OptionState.Correct, OptionState.MissedCorrect -> Quad(ext.correct, ext.correctContainer, ext.correct, Color.White)
        OptionState.Incorrect -> Quad(ext.incorrect, ext.incorrectContainer, ext.incorrect, Color.White)
        OptionState.Dimmed -> Quad(scheme.outlineVariant, scheme.surfaceContainer, scheme.surfaceVariant, scheme.onSurfaceVariant)
    }
    val animatedBg by animateColorAsState(bg, label = "optionBg")
    val selected = state == OptionState.Selected || state == OptionState.Correct || state == OptionState.Incorrect
    val stateText = when (state) {
        OptionState.Correct -> stringResource(R.string.ui_correct_answer) + ", " + stringResource(R.string.ui_your_answer)
        OptionState.MissedCorrect -> stringResource(R.string.ui_correct_answer)
        OptionState.Incorrect -> stringResource(R.string.ui_your_answer)
        else -> null
    }
    val interaction = when {
        onClick == null -> Modifier
        multiSelect -> Modifier.toggleable(selected, role = Role.Checkbox) { onClick() }
        else -> Modifier.selectable(selected, role = Role.RadioButton) { onClick() }
    }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.small)
            .background(animatedBg)
            .border(BorderStroke(if (state == OptionState.Neutral || state == OptionState.Dimmed) 1.dp else 1.5.dp, borderColor), MaterialTheme.shapes.small)
            .then(interaction)
            .semantics { stateText?.let { stateDescription = it } }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).clip(if (multiSelect) RoundedCornerShape(8.dp) else CircleShape).background(badgeBg),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                OptionState.Correct, OptionState.MissedCorrect -> Icon(Icons.Rounded.Check, null, tint = badgeFg, modifier = Modifier.size(18.dp))
                OptionState.Incorrect -> Icon(Icons.Rounded.Close, null, tint = badgeFg, modifier = Modifier.size(18.dp))
                else -> Text(letter, style = MaterialTheme.typography.labelLarge, color = badgeFg)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (state == OptionState.Dimmed) scheme.onSurfaceVariant else scheme.onSurface,
            )
            imageUrl?.let {
                AsyncImage(it, contentDescription = text, modifier = Modifier.padding(top = 6.dp).heightIn(max = 120.dp))
            }
        }
    }
}

private data class Quad(val a: Color, val b: Color, val c: Color, val d: Color)

@Composable
fun PassageCard(title: String?, text: String, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    Column(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { expanded = !expanded }.padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title ?: stringResource(R.string.ui_passage), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
        }
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn()) {
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** Formulas in readable linear notation in a monospace box that scrolls sideways if long. */
@Composable
fun FormulaBox(expression: String, modifier: Modifier = Modifier, description: String? = null) {
    val label = stringResource(R.string.ui_formula)
    Box(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics { contentDescription = "$label: ${description ?: expression}" },
    ) {
        Text(expression, style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Suppress("unused")
private val keepRemember = @Composable { remember { 0 } }
