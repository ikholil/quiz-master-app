package app.prostuti.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.prostuti.core.designsystem.theme.Spacing

/** Flat card with a hairline border — calmer than shadows and cheaper to draw on low-end GPUs. */
@Composable
fun PtCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    border: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(Spacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    val stroke = if (border) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    val shape = MaterialTheme.shapes.medium
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, border = stroke) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = color, border = stroke) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(top = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (actionLabel != null && onAction != null) {
            PtButton(actionLabel, onAction, style = PtButtonStyle.Text)
        }
    }
}

/** Circle with a tinted icon; used as a leading visual in rows and tiles. */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    size: Dp = 40.dp,
) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** Standard tappable list row with a 56dp minimum height (≥48dp touch target). */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val row: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) { leading(); Spacer(Modifier.width(Spacing.md)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (trailing != null) { Spacer(Modifier.width(Spacing.sm)); trailing() }
            if (showChevron) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (onClick != null) Surface(onClick = onClick, modifier = modifier, color = Color.Transparent) { row() }
    else Box(modifier) { row() }
}

/** Compact status pill, e.g. "Official", "Hard", "Offline". */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    container: Color = MaterialTheme.colorScheme.surfaceVariant,
    icon: ImageVector? = null,
) {
    Row(
        modifier.clip(CircleShape).background(container).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
    }
}

@Composable
fun Dot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

/** Big number with a small label beneath — used in result and analytics summaries. */
@Composable
fun StatBlock(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"), color = valueColor)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HairlineDivider(modifier: Modifier = Modifier, inset: Dp = 0.dp) {
    Box(
        modifier.fillMaxWidth().padding(start = inset).heightIn(min = 1.dp, max = 1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Suppress("unused")
private val PillRole = Role.Button
