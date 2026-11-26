package app.prostuti.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.prostuti.core.designsystem.component.Pill
import app.prostuti.core.designsystem.theme.ProstutiThemeExt
import app.prostuti.core.designsystem.theme.Spacing
import app.prostuti.core.model.PlanTask
import app.prostuti.core.model.PlanTaskStatus
import app.prostuti.core.model.PlanTaskType

fun PlanTaskType.icon() = when (this) {
    PlanTaskType.LEARN -> Icons.AutoMirrored.Rounded.MenuBook
    PlanTaskType.PRACTICE -> Icons.Rounded.EditNote
    PlanTaskType.REVISE -> Icons.Rounded.Replay
    PlanTaskType.MOCK -> Icons.Rounded.Timer
    PlanTaskType.FLASHCARDS -> Icons.Rounded.Style
}

@Composable
fun PlanTaskType.label() = stringResource(
    when (this) {
        PlanTaskType.LEARN -> R.string.ui_task_learn
        PlanTaskType.PRACTICE -> R.string.ui_task_practice
        PlanTaskType.REVISE -> R.string.ui_task_revise
        PlanTaskType.MOCK -> R.string.ui_task_mock
        PlanTaskType.FLASHCARDS -> R.string.ui_task_flashcards
    },
)

/**
 * A plan task: a round check on the left (toggle), the task and its *reason* in the middle.
 * Tapping the body opens the task; tapping the circle completes it.
 */
@Composable
fun PlanTaskRow(task: PlanTask, onToggle: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val done = task.status == PlanTaskStatus.DONE
    val ext = ProstutiThemeExt.colors
    Surface(onClick = onOpen, modifier = modifier.fillMaxWidth(), color = Color.Transparent) {
        Row(Modifier.padding(vertical = Spacing.sm), verticalAlignment = Alignment.Top) {
            val toggleLabel = stringResource(if (done) R.string.ui_mark_undone else R.string.ui_mark_done)
            Box(
                Modifier.size(48.dp).toggleable(done, role = Role.Checkbox, onValueChange = { onToggle() })
                    .semantics { contentDescription = toggleLabel },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(26.dp).clip(CircleShape)
                        .background(if (done) ext.correct else Color.Transparent)
                        .border(2.dp, if (done) ext.correct else MaterialTheme.colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { if (done) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
            }
            Column(Modifier.weight(1f).padding(top = 4.dp, end = Spacing.sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(task.type.icon(), null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text(task.type.label() + " · " + minutesLabel(task.minutes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    when (task.status) {
                        PlanTaskStatus.MISSED -> Pill(stringResource(R.string.ui_status_missed), color = ext.incorrect, container = ext.incorrectContainer)
                        PlanTaskStatus.RESCHEDULED -> Pill(stringResource(R.string.ui_status_moved))
                        else -> Unit
                    }
                }
                Text(
                    task.title.text(),
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                    color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                Text(stringResource(R.string.ui_why, task.reason.text()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
