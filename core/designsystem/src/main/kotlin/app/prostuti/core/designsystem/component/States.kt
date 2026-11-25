package app.prostuti.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.prostuti.core.designsystem.theme.Spacing

/**
 * Skeleton block. A gentle alpha pulse instead of a moving gradient shimmer: same perceived
 * effect, one animated float, no per-frame shader allocation on cheap phones.
 */
@Composable
fun SkeletonBox(modifier: Modifier = Modifier, height: Dp = 16.dp, corner: Dp = 8.dp) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(0.45f, 0.9f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    Box(
        modifier.height(height).alpha(alpha).clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

/** A reasonable stand-in for list/card screens while loading. */
@Composable
fun SkeletonList(modifier: Modifier = Modifier, rows: Int = 5) {
    Column(modifier.padding(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        SkeletonBox(Modifier.fillMaxWidth(0.55f), height = 24.dp)
        SkeletonBox(Modifier.fillMaxWidth(), height = 120.dp, corner = 14.dp)
        repeat(rows) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonBox(Modifier.size(40.dp), height = 40.dp, corner = 12.dp)
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SkeletonBox(Modifier.fillMaxWidth(0.8f), height = 14.dp)
                    SkeletonBox(Modifier.fillMaxWidth(0.5f), height = 12.dp)
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = Spacing.xxxl, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(icon, size = 64.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant, container = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(Modifier.height(Spacing.lg))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.xs))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 320.dp))
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.xl))
            PtButton(actionLabel, onAction, style = PtButtonStyle.Tonal)
        }
    }
}

@Composable
fun ErrorState(
    icon: ImageVector,
    title: String,
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().semantics { liveRegion = LiveRegionMode.Polite }, contentAlignment = Alignment.Center) {
        EmptyState(icon, title, message, actionLabel = retryLabel, onAction = onRetry)
    }
}

/** Slim banner shown above content while offline; explains what still works. */
@Composable
fun OfflineBanner(visible: Boolean, message: String, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible, modifier, enter = expandVertically(), exit = shrinkVertically()) {
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.inverseSurface)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.CloudOff, null, tint = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text(message, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.inverseOnSurface)
        }
    }
}
