package app.prostuti.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

enum class PtButtonStyle { Primary, Tonal, Outline, Text }

/**
 * The single button used across the app. 52dp tall primary buttons are easy to hit on small phones;
 * [loading] keeps the label width stable and blocks double submission.
 */
@Composable
fun PtButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PtButtonStyle = PtButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val content: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                Spacer(Modifier.width(10.dp))
            } else if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text)
            if (trailingIcon != null && !loading) {
                Spacer(Modifier.width(8.dp))
                Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
    val m = modifier
        .heightIn(min = if (style == PtButtonStyle.Text) 40.dp else 52.dp)
        .semantics { if (loading) stateDescription = "Loading" }
    val padding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
    val isEnabled = enabled && !loading
    when (style) {
        PtButtonStyle.Primary -> Button(onClick, m, isEnabled, contentPadding = padding) { content() }
        PtButtonStyle.Tonal -> FilledTonalButton(onClick, m, isEnabled, contentPadding = padding) { content() }
        PtButtonStyle.Outline -> OutlinedButton(onClick, m, isEnabled, contentPadding = padding) { content() }
        PtButtonStyle.Text -> TextButton(onClick, m, isEnabled, contentPadding = PaddingValues(horizontal = 12.dp)) { content() }
    }
}
