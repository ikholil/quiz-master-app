package app.prostuti.core.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.prostuti.core.designsystem.component.ListRow
import app.prostuti.core.designsystem.component.PtCard
import app.prostuti.core.designsystem.theme.ProstutiThemeExt
import app.prostuti.core.designsystem.theme.Spacing
import app.prostuti.core.model.Question
import app.prostuti.core.model.SourceReference

/** Explanation, per-option reasoning and sources. Shown only when answers may be revealed. */
@Composable
fun SolutionView(question: Question, modifier: Modifier = Modifier) {
    val solution = question.solution ?: return
    val ext = ProstutiThemeExt.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        PtCard(color = ext.correctContainer.copy(alpha = 0.6f), border = false) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lightbulb, null, tint = ext.correct, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.ui_explanation), style = MaterialTheme.typography.titleSmall, color = ext.correct)
            }
            Spacer(Modifier.padding(top = 6.dp))
            Text(solution.explanation.text(), style = MaterialTheme.typography.bodyMedium)
            solution.modelAnswer?.let {
                Spacer(Modifier.padding(top = 10.dp))
                Text(stringResource(R.string.ui_model_answer), style = MaterialTheme.typography.labelLarge)
                Text(it.text(), style = MaterialTheme.typography.bodyMedium)
            }
        }
        val wrongOnes = question.options.filter { it.id !in solution.correctOptionIds && solution.optionExplanations[it.id] != null }
        if (wrongOnes.isNotEmpty()) {
            PtCard {
                Text(stringResource(R.string.ui_why_option_wrong), style = MaterialTheme.typography.titleSmall)
                wrongOnes.forEach { option ->
                    Column(Modifier.padding(top = 10.dp)) {
                        Text(option.text.text(), style = MaterialTheme.typography.labelLarge, color = ext.incorrect)
                        Text(solution.optionExplanations.getValue(option.id).text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (question.examReferences.isNotEmpty()) {
            Column {
                Text(stringResource(R.string.ui_asked_in), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                question.examReferences.forEach { ref ->
                    Text("• " + listOfNotNull(ref.examName.text(), ref.stage, ref.year?.toString()).joinToString(", ").loc(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        SourceList(question.sources)
    }
}

@Composable
fun SourceList(sources: List<SourceReference>, modifier: Modifier = Modifier) {
    if (sources.isEmpty()) return
    val context = LocalContext.current
    Column(modifier.fillMaxWidth()) {
        Text(stringResource(R.string.ui_sources), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        sources.forEach { source ->
            ListRow(
                title = source.title,
                subtitle = listOfNotNull(source.publisher, source.publishedAt?.let { it.atZone(java.time.ZoneId.of("Asia/Dhaka")).toLocalDate().toString() }).joinToString(" · "),
                trailing = { if (source.url != null) Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                onClick = source.url?.let { url -> { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } },
            )
        }
    }
}
