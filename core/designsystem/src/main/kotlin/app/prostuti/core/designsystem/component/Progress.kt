package app.prostuti.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.prostuti.core.designsystem.theme.ProstutiThemeExt

/** Ring progress with an optional center slot. Announced to TalkBack as a progress bar. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    stroke: Dp = 7.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = ProstutiThemeExt.colors.chartTrack,
    center: @Composable () -> Unit = {},
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "ring")
    Box(
        modifier.size(size).semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            val s = stroke.toPx()
            val arcSize = Size(this.size.width - s, this.size.height - s)
            val topLeft = Offset(s / 2, s / 2)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(s))
            drawArc(color, -90f, 360f * animated, false, topLeft, arcSize, style = Stroke(s, cap = StrokeCap.Round))
        }
        center()
    }
}

/** Thin rounded meter bar. */
@Composable
fun MeterBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = ProstutiThemeExt.colors.chartTrack,
    height: Dp = 6.dp,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(600), label = "meter")
    Box(
        modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height)).semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
        },
    ) {
        Canvas(Modifier.matchParentSize()) {
            val r = CornerRadius(size.height / 2)
            drawRoundRect(track, cornerRadius = r)
            if (animated > 0f) drawRoundRect(color, size = Size(size.width * animated, size.height), cornerRadius = r)
        }
    }
}

/** Segmented bar for distributions, e.g. correct / wrong / skipped. */
@Composable
fun SegmentedMeter(
    segments: List<Pair<Float, Color>>,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
) {
    val total = segments.sumOf { it.first.toDouble() }.toFloat().takeIf { it > 0f } ?: 1f
    Row(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        segments.filter { it.first > 0f }.forEach { (value, color) ->
            Box(Modifier.weight(value / total).fillMaxHeight().clip(RoundedCornerShape(2.dp)).then(Modifier.semantics { }).let { m ->
                m
            }) {
                Canvas(Modifier.matchParentSize()) { drawRect(color) }
            }
        }
    }
}

/** Minimal vertical bar chart (e.g. minutes studied per weekday). Highlights [highlightIndex]. */
@Composable
fun MiniBars(
    values: List<Float>,
    modifier: Modifier = Modifier,
    highlightIndex: Int? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    muted: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    val max = values.maxOrNull()?.takeIf { it > 0f } ?: 1f
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val gap = 6.dp.toPx()
        val barW = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { i, v ->
            val h = (v / max).coerceAtLeast(0.04f) * size.height
            drawRoundRect(
                color = if (i == highlightIndex) color else muted,
                topLeft = Offset(i * (barW + gap), size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
        }
    }
}

/** Simple trend line with dots; values normalised to the chart height. */
@Composable
fun TrendLine(
    values: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    grid: Color = ProstutiThemeExt.colors.chartTrack,
    minValue: Float? = null,
    maxValue: Float? = null,
) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val lo = minValue ?: values.min()
        val hi = (maxValue ?: values.max()).let { if (it == lo) lo + 1f else it }
        repeat(3) { i ->
            val y = size.height * i / 2f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { i, v -> Offset(i * stepX, size.height - (v - lo) / (hi - lo) * size.height) }
        for (i in 0 until points.size - 1) {
            drawLine(color, points[i], points[i + 1], strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
        }
        drawCircle(color, 4.dp.toPx(), points.last())
        drawCircle(Color.White, 2.dp.toPx(), points.last())
    }
}
