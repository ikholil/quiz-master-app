package app.prostuti.core.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.prostuti.core.designsystem.component.ListRow
import app.prostuti.core.designsystem.component.IconBadge
import app.prostuti.core.designsystem.theme.ProstutiThemeExt
import app.prostuti.core.designsystem.theme.Spacing
import app.prostuti.core.model.CalloutKind
import app.prostuti.core.model.ContentBlock
import coil.compose.AsyncImage

/** Renders typed learning-material blocks natively (fast, offline-friendly, accessible). */
@Composable
fun ContentBlocks(blocks: List<ContentBlock>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        blocks.forEach { ContentBlockView(it) }
    }
}

@Composable
fun ContentBlockView(block: ContentBlock) {
    val context = LocalContext.current
    val open: (String) -> Unit = { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    when (block) {
        is ContentBlock.Heading -> Text(
            block.text.text(),
            style = if (block.level <= 2) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = Spacing.sm).semantics { heading() },
        )
        is ContentBlock.Paragraph -> Text(block.text.text(), style = MaterialTheme.typography.bodyLarge)
        is ContentBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            block.items.forEach { item ->
                Row {
                    Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(18.dp))
                    Text(item.text(), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        is ContentBlock.Callout -> Callout(block.kind, block.text.text())
        is ContentBlock.Image -> Column {
            AsyncImage(
                block.url, contentDescription = block.altText.text(), contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant),
            )
            block.caption?.let { Text(it.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
        }
        is ContentBlock.Table -> TableView(block)
        is ContentBlock.Formula -> FormulaBox(block.expression, description = block.description.text())
        is ContentBlock.Audio -> MediaRow(Icons.Rounded.Headphones, block.title.text(), stringResource(R.string.ui_listen)) { open(block.url) }
        is ContentBlock.Video -> MediaRow(Icons.Rounded.PlayCircle, block.title.text(), stringResource(R.string.ui_watch)) { open(block.url) }
        is ContentBlock.Pdf -> MediaRow(Icons.Rounded.PictureAsPdf, block.title.text(), stringResource(R.string.ui_open_pdf)) { open(block.url) }
    }
}

@Composable
private fun Callout(kind: CalloutKind, text: String) {
    val ext = ProstutiThemeExt.colors
    val (label, icon, fg, bg) = when (kind) {
        CalloutKind.TIP -> Quadruple(R.string.ui_tip, Icons.Rounded.Lightbulb, ext.verified, ext.verifiedContainer)
        CalloutKind.REMEMBER -> Quadruple(R.string.ui_remember, Icons.Rounded.PushPin, ext.correct, ext.correctContainer)
        CalloutKind.EXAM_TRAP -> Quadruple(R.string.ui_exam_trap, Icons.Rounded.PriorityHigh, ext.community, ext.communityContainer)
    }
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(bg.copy(alpha = 0.7f)).padding(14.dp),
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(stringResource(label), style = MaterialTheme.typography.labelLarge, color = fg)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private data class Quadruple<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
private fun TableView(table: ContentBlock.Table) {
    val line = MaterialTheme.colorScheme.outlineVariant
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).border(1.dp, line, MaterialTheme.shapes.small)
            .horizontalScroll(rememberScrollState()),
    ) {
        TableRow(table.header.map { it.text() }, header = true)
        table.rows.forEach { row -> TableRow(row.map { it.text() }, header = false) }
    }
}

@Composable
private fun TableRow(cells: List<String>, header: Boolean) {
    Row(Modifier.background(if (header) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)) {
        cells.forEach { cell ->
            Text(
                cell,
                style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                modifier = Modifier.widthIn(min = 110.dp, max = 220.dp).padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun MediaRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, action: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))) {
        ListRow(title = title, subtitle = action, leading = { IconBadge(icon) }, onClick = onClick, showChevron = true)
    }
}

@Suppress("unused") private val align = Alignment.Center
