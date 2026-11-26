package app.prostuti.feature.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.prostuti.core.common.AppResult
import app.prostuti.core.designsystem.component.*
import app.prostuti.core.designsystem.theme.ProstutiThemeExt
import app.prostuti.core.designsystem.theme.Spacing
import app.prostuti.core.designsystem.theme.tabular
import app.prostuti.core.domain.repository.PlannerRepository
import app.prostuti.core.model.*
import app.prostuti.core.ui.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@Serializable data object PlannerRoute

fun NavGraphBuilder.plannerGraph(@Suppress("UNUSED_PARAMETER") navController: NavController) {
    composable<PlannerRoute> { PlannerScreen() }
}

@HiltViewModel
class PlannerViewModel @Inject constructor(private val repo: PlannerRepository) : LoadViewModel<StudyPlan>() {
    private val _selected = MutableStateFlow(LocalDate.now())
    val selected = _selected.asStateFlow()
    init { load() }
    override suspend fun fetch() = repo.plan(null)

    fun select(date: LocalDate) { _selected.value = date }

    fun toggle(task: PlanTask) {
        val next = if (task.status == PlanTaskStatus.DONE) PlanTaskStatus.PENDING else PlanTaskStatus.DONE
        update { plan -> plan.copy(days = plan.days.map { d -> d.copy(tasks = d.tasks.map { if (it.id == task.id) it.copy(status = next) else it }) }) }
        launchAction { repo.setTaskStatus(task.id, next) }
    }

    fun recover(onDone: () -> Unit) = viewModelScope.launch {
        when (val r = repo.recoverMissedTasks()) {
            is AppResult.Success -> { update { r.data }; onDone() }
            is AppResult.Failure -> report(r.error)
        }
    }

    fun regenerate(minutes: Int, onDone: () -> Unit) = viewModelScope.launch {
        val date = (state.value as? UiState.Success)?.data?.examDate
        when (val r = repo.regenerate(minutes, date)) {
            is AppResult.Success -> { update { r.data }; onDone() }
            is AppResult.Failure -> report(r.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlannerScreen(viewModel: PlannerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var adjusting by rememberSaveable { mutableStateOf(false) }
    var monthView by rememberSaveable { mutableStateOf(false) }
    val recovered = stringResource(R.string.pl_recovered)
    val regenerated = stringResource(R.string.pl_regenerated)
    MessagesEffect(viewModel.messages, snackbar)
    PtScaffold(
        stringResource(R.string.pl_title), onBack = { nav.navigate(Destination.Back) }, snackbarHostState = snackbar,
        actions = { IconButton(onClick = { adjusting = true }) { Icon(Icons.Rounded.Tune, stringResource(R.string.pl_adjust)) } },
    ) { padding ->
        StateContent(state, onRetry = viewModel::load, modifier = Modifier.padding(padding)) { plan ->
            val day = plan.days.firstOrNull { it.date == selected }
            ReadableWidth {
                LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(vertical = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    item {
                        Column(Modifier.screenGutter()) {
                            Text(plan.examName.text(), style = MaterialTheme.typography.titleMedium)
                            plan.examDate?.let { Text(stringResource(R.string.pl_countdown, daysUntil(it).loc(), it.pretty()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
                        }
                    }
                    if (plan.missedTaskCount > 0) item {
                        PtCard(Modifier.fillMaxWidth().screenGutter(), color = ProstutiThemeExt.colors.communityContainer.copy(alpha = 0.6f), border = false) {
                            Text(stringResource(R.string.pl_missed_title, plan.missedTaskCount.loc()), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.pl_missed_body), style = MaterialTheme.typography.bodySmall)
                            PtButton(stringResource(R.string.pl_recover), { viewModel.recover { scope.launch { snackbar.showSnackbar(recovered) } } }, style = PtButtonStyle.Text, leadingIcon = Icons.Rounded.EventRepeat)
                        }
                    }
                    item {
                        Box(Modifier.screenGutter()) {
                            SegmentedControl(listOf(false, true), monthView, { monthView = it }, label = { stringResource(if (it) R.string.pl_month else R.string.pl_week) })
                        }
                    }
                    item {
                        if (monthView) MonthGrid(plan, selected, viewModel::select)
                        else LazyRow(contentPadding = PaddingValues(horizontal = Spacing.gutter), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            items(plan.days, key = { it.date.toString() }) { d -> DayChip(d, d.date == selected) { viewModel.select(d.date) } }
                        }
                    }
                    item {
                        Column(Modifier.screenGutter()) {
                            Text(if (selected == LocalDate.now()) stringResource(app.prostuti.core.ui.R.string.ui_today) else selected.pretty("EEEE, d MMMM"), style = MaterialTheme.typography.titleMedium)
                            if (day != null && day.tasks.isNotEmpty()) {
                                Text(stringResource(R.string.pl_day_summary, minutesLabel(day.completedMinutes), minutesLabel(day.plannedMinutes)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(Spacing.sm))
                                MeterBar(day.completedMinutes.toFloat() / day.plannedMinutes.coerceAtLeast(1))
                            }
                        }
                    }
                    item {
                        if (day == null || day.tasks.isEmpty()) Text(stringResource(R.string.pl_no_tasks), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.screenGutter())
                        else PtCard(Modifier.fillMaxWidth().screenGutter(), contentPadding = PaddingValues(Spacing.xs)) {
                            day.tasks.forEachIndexed { i, t ->
                                if (i > 0) HairlineDivider(inset = 56.dp)
                                PlanTaskRow(t, onToggle = { viewModel.toggle(t) }, onOpen = { nav.navigate(t.destination()) })
                            }
                        }
                    }
                    if (plan.adjustments.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.pl_changes), modifier = Modifier.screenGutter()) }
                        items(plan.adjustments, key = { it.id }) { a ->
                            ListRow(a.summary.text(), subtitle = a.date.pretty(), leading = { IconBadge(Icons.Rounded.History, size = 36.dp) }, modifier = Modifier.padding(horizontal = Spacing.xs))
                        }
                    }
                    item { Text(stringResource(R.string.pl_explain), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.screenGutter()) }
                }
            }
            if (adjusting) {
                var minutes by rememberSaveable { mutableIntStateOf(60) }
                ModalBottomSheet(onDismissRequest = { adjusting = false }, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.padding(horizontal = Spacing.gutter).padding(bottom = Spacing.xxl).navigationBarsPadding()) {
                        Text(stringResource(R.string.pl_daily_time), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(Spacing.md))
                        SegmentedControl(listOf(30, 60, 90, 120, 180), minutes, { minutes = it }, label = { minutesLabel(it) })
                        Spacer(Modifier.height(Spacing.xl))
                        PtButton(stringResource(R.string.pl_regenerate), { viewModel.regenerate(minutes) { adjusting = false; scope.launch { snackbar.showSnackbar(regenerated) } } }, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun DayChip(day: StudyDay, selected: Boolean, onClick: () -> Unit) {
    val progress = day.completedMinutes.toFloat() / day.plannedMinutes.coerceAtLeast(1)
    val missed = day.tasks.any { it.status == PlanTaskStatus.MISSED }
    Column(
        Modifier.width(56.dp).clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .selectable(selected, role = Role.Tab, onClick = onClick).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        Text(day.date.pretty("EEE"), style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.8f))
        Text(day.date.dayOfMonth.loc(), style = MaterialTheme.typography.titleMedium.tabular, color = fg)
        Spacer(Modifier.height(4.dp))
        Dot(
            when {
                missed -> ProstutiThemeExt.colors.incorrect
                progress >= 1f -> ProstutiThemeExt.colors.correct
                progress > 0f -> ProstutiThemeExt.colors.community
                else -> fg.copy(alpha = 0.25f)
            },
            size = 6.dp,
        )
    }
}

@Composable
private fun MonthGrid(plan: StudyPlan, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val month = YearMonth.from(selected)
    val byDate = plan.days.associateBy { it.date }
    val offset = month.atDay(1).dayOfWeek.value % 7 // Sunday-first, as in Bangladesh
    val cells = List(offset) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    PtCard(Modifier.fillMaxWidth().screenGutter()) {
        Text(selected.pretty("MMMM yyyy"), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(Spacing.sm))
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (date != null) {
                            val d = byDate[date]
                            val p = d?.let { it.completedMinutes.toFloat() / it.plannedMinutes.coerceAtLeast(1) } ?: 0f
                            val isSel = date == selected
                            Box(
                                Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isSel -> MaterialTheme.colorScheme.primary
                                            d == null -> MaterialTheme.colorScheme.surface
                                            p >= 1f -> ProstutiThemeExt.colors.correctContainer
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                    )
                                    .selectable(isSel, role = Role.Button) { onSelect(date) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(date.dayOfMonth.loc(), style = MaterialTheme.typography.labelMedium.tabular,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
