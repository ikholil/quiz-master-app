package app.prostuti.core.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.prostuti.core.designsystem.component.IconBadge
import app.prostuti.core.designsystem.component.PtCard
import app.prostuti.core.model.Recommendation

/** A recommendation always shows *why* it was made, in the student's language. */
@Composable
fun RecommendationCard(recommendation: Recommendation, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PtCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            IconBadge(Icons.Rounded.AutoAwesome, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                Text(recommendation.title.text(), style = MaterialTheme.typography.titleSmall)
                Text(
                    recommendation.reason.text(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
        }
    }
}
