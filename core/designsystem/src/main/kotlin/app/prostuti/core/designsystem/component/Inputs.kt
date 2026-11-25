package app.prostuti.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun PtTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    error: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon?.let { { Icon(it, null) } },
        trailingIcon = trailing,
        supportingText = (error ?: supportingText)?.let { { Text(it) } },
        isError = error != null,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

@Composable
fun PtFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        leadingIcon = icon?.let { { Icon(it, null, Modifier.padding(start = 2.dp)) } },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true, selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

/** iOS-style segmented control built on accessible selectable semantics. */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).padding(3.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val bg by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceVariant, label = "seg")
            Box(
                Modifier.weight(1f).heightIn(min = 40.dp).clip(RoundedCornerShape(10.dp)).background(bg)
                    .selectable(isSelected, role = Role.Tab) { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Selectable card used for single/multi choice in onboarding and settings. */
@Composable
fun ChoiceCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    role: Role = Role.RadioButton,
) {
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceContainer
    Row(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(bg)
            .border(BorderStroke(if (selected) 1.5.dp else 1.dp, border), MaterialTheme.shapes.medium)
            .selectable(selected, role = role, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            IconBadge(icon, size = 36.dp)
            Box(Modifier.padding(start = 12.dp))
        }
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        androidx.compose.material3.RadioButton(selected = selected, onClick = null, modifier = Modifier.height(24.dp))
    }
}
