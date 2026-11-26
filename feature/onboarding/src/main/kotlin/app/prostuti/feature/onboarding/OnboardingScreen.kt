package app.prostuti.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.prostuti.core.designsystem.component.*
import app.prostuti.core.designsystem.theme.Spacing
import app.prostuti.core.model.*
import app.prostuti.core.ui.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
internal fun OnboardingScreen(
    onFinished: () -> Unit,
    onExit: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.finished.collect { onFinished() } }
    val goBack = { if (!viewModel.back()) onExit() }
    BackHandler(enabled = state.step.ordinal > 0 || state.editMode) { goBack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(Modifier.statusBarsPadding().padding(horizontal = Spacing.sm, vertical = Spacing.xs)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.step.ordinal > 0 || state.editMode) {
                        IconButton(onClick = goBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.ob_back)) }
                    } else Spacer(Modifier.size(48.dp))
                    Text(
                        stringResource(R.string.ob_step, (state.step.ordinal + 1).loc(), OnboardingStep.entries.size.loc()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MeterBar((state.step.ordinal + 1f) / OnboardingStep.entries.size, Modifier.padding(horizontal = Spacing.md), height = 4.dp)
            }
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(Modifier.navigationBarsPadding().imePadding().padding(Spacing.gutter)) {
                    state.error?.let { Text(errorMessage(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    PtButton(
                        text = stringResource(
                            when {
                                state.step != OnboardingStep.PLAN -> R.string.ob_next
                                state.editMode -> R.string.ob_save
                                else -> R.string.ob_start
                            },
                        ),
                        onClick = viewModel::next,
                        enabled = state.canContinue,
                        loading = state.saving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = state.step,
            transitionSpec = { (slideInHorizontally { it / 6 } + fadeIn()) togetherWith fadeOut() },
            modifier = Modifier.padding(padding),
            label = "step",
        ) { step ->
            ReadableWidth {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.gutter, vertical = Spacing.lg)) {
                    when (step) {
                        OnboardingStep.ABOUT -> AboutStep(state, viewModel)
                        OnboardingStep.EXAMS -> ExamsStep(state, viewModel)
                        OnboardingStep.DATE -> DateStep(state, viewModel)
                        OnboardingStep.TIME -> TimeStep(state, viewModel)
                        OnboardingStep.LEVEL -> LevelStep(state, viewModel)
                        OnboardingStep.SUBJECTS -> SubjectsStep(state, viewModel)
                        OnboardingStep.PLAN -> PlanStep(state, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
    Spacer(Modifier.height(Spacing.xs))
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(Spacing.xxl))
}

@Composable
private fun AboutStep(state: OnboardingState, vm: OnboardingViewModel) {
    StepHeader(stringResource(R.string.ob_about_title), stringResource(R.string.ob_about_subtitle))
    PtTextField(state.name, vm::onName, stringResource(R.string.ob_name))
    Spacer(Modifier.height(Spacing.xl))
    Text(stringResource(R.string.ob_language), style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(Spacing.sm))
    SegmentedControl(
        listOf(AppLanguage.BENGALI, AppLanguage.ENGLISH), state.language, vm::onLanguage,
        label = { if (it == AppLanguage.BENGALI) "বাংলা" else "English" },
    )
}

private fun categoryIcon(c: ExamCategory) = when (c) {
    ExamCategory.BCS, ExamCategory.GOVERNMENT_JOB -> Icons.Rounded.AccountBalance
    ExamCategory.BANK -> Icons.Rounded.Work
    ExamCategory.NTRCA, ExamCategory.PRIMARY_TEACHER -> Icons.Rounded.MenuBook
    ExamCategory.UNIVERSITY_ADMISSION -> Icons.Rounded.School
    ExamCategory.OTHER -> Icons.Rounded.AutoStories
}

@Composable
private fun ExamsStep(state: OnboardingState, vm: OnboardingViewModel) {
    StepHeader(stringResource(R.string.ob_exams_title), stringResource(R.string.ob_exams_subtitle))
    StateContent(state.exams, onRetry = { vm.loadExams() }, loading = { SkeletonList(rows = 4) }) { exams ->
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            exams.forEach { exam ->
                val selected = exam.id in state.selectedExamIds
                val primary = selected && state.primaryExamId == exam.id && state.selectedExamIds.size > 1
                ChoiceCard(
                    title = exam.name.text(),
                    subtitle = exam.authority.text(),
                    selected = selected,
                    onClick = { vm.toggleExam(exam.id) },
                    icon = categoryIcon(exam.category),
                    role = Role.Checkbox,
                    badge = if (primary) ({ Pill(stringResource(R.string.ob_primary), color = MaterialTheme.colorScheme.onPrimary, container = MaterialTheme.colorScheme.primary, icon = Icons.Rounded.Star) }) else null,
                )
                if (selected && !primary && state.selectedExamIds.size > 1) {
                    PtButton(stringResource(R.string.ob_make_primary), { vm.setPrimary(exam.id) }, style = PtButtonStyle.Text, leadingIcon = Icons.Rounded.StarBorder)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateStep(state: OnboardingState, vm: OnboardingViewModel) {
    StepHeader(stringResource(R.string.ob_date_title, state.primaryExam?.shortName ?: ""), stringResource(R.string.ob_date_subtitle))
    var picking by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    ChoiceCard(
        title = state.examDate?.let { stringResource(R.string.ob_date_selected, it.pretty(), daysUntil(it).loc()) } ?: stringResource(R.string.ob_date_pick),
        selected = !state.dateUnknown && state.examDate != null,
        onClick = { picking = true },
        icon = Icons.Rounded.CalendarMonth,
    )
    Spacer(Modifier.height(Spacing.sm))
    ChoiceCard(
        title = stringResource(R.string.ob_date_unknown),
        subtitle = stringResource(R.string.ob_date_unknown_desc),
        selected = state.dateUnknown,
        onClick = { vm.setDateUnknown(true) },
        icon = Icons.Rounded.EventBusy,
    )
    if (picking) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (state.examDate ?: today.plusMonths(3)).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= today.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { vm.setDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    picking = false
                }) { Text("OK") }
            },
        ) { DatePicker(pickerState) }
    }
}

@Composable
private fun TimeStep(state: OnboardingState, vm: OnboardingViewModel) {
    StepHeader(stringResource(R.string.ob_time_title), stringResource(R.string.ob_time_subtitle))
    val options = listOf(30 to R.string.ob_time_30, 60 to R.string.ob_time_60, 120 to R.string.ob_time_120, 180 to R.string.ob_time_180)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        options.forEach { (minutes, desc) ->
            ChoiceCard(minutesLabel(minutes), state.dailyMinutes == minutes, { vm.setMinutes(minutes) }, subtitle = stringResource(desc))
        }
    }
}

@Composable
private fun LevelStep(state: OnboardingState, vm: OnboardingViewModel) {
    StepHeader(stringResource(R.string.ob_level_title), stringResource(R.string.ob_level_subtitle))
    val options = listOf(
        Triple(PreparationLevel.BEGINNER, R.string.ob_level_beginner, R.string.ob_level_beginner_desc),
        Triple(PreparationLevel.INTERMEDIATE, R.string.ob_level_intermediate, R.string.ob_level_intermediate_desc),
        Triple(PreparationLevel.ADVANCED, R.string.ob_level_advanced, R.string.ob_level_advanced_desc),
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        options.forEach { (level, title, desc) ->
            ChoiceCard(stringResource(title), state.level == level, { vm.setLevel(level) }, subtitle = stringResource(desc), icon = Icons.Rounded.FitnessCenter)
        }
    }
}

@Composable
private fun SubjectsStep(state: OnboardingState, vm: OnboardingViewModel) {
    StepHeader(stringResource(R.string.ob_subjects_title), stringResource(R.string.ob_subjects_subtitle))
    StateContent(state.subjects, onRetry = {}, loading = { SkeletonList(rows = 5) }) { subjects ->
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            subjects.forEach { subject ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(subject.name.text(), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        subject.marks?.let { Text(stringResource(R.string.ob_marks, it.loc()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Spacer(Modifier.height(6.dp))
                    SegmentedControl(
                        SubjectStrength.entries, state.strengths[subject.id] ?: SubjectStrength.OK, { vm.setStrength(subject.id, it) },
                        label = {
                            stringResource(when (it) { SubjectStrength.WEAK -> R.string.ob_weak; SubjectStrength.OK -> R.string.ob_ok; SubjectStrength.STRONG -> R.string.ob_strong })
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanStep(state: OnboardingState, vm: OnboardingViewModel) {
    StepHeader(stringResource(R.string.ob_plan_title), stringResource(R.string.ob_plan_subtitle))
    StateContent(state.plan, onRetry = { vm.buildPlan() }, loading = {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 48.dp)) {
            CircularProgressIndicator()
            Spacer(Modifier.height(Spacing.md))
            Text(stringResource(R.string.ob_plan_loading), style = MaterialTheme.typography.bodyMedium)
        }
    }) { tasks ->
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            tasks.groupBy { it.date }.forEach { (date, dayTasks) ->
                Text(if (date == LocalDate.now()) stringResource(app.prostuti.core.ui.R.string.ui_today) else date.pretty("EEEE, d MMM"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                dayTasks.forEach { task ->
                    PtCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(task.title.text(), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Pill(minutesLabel(task.minutes))
                        }
                        Text(task.reason.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Suppress("unused") private val bg = Modifier.background(androidx.compose.ui.graphics.Color.Transparent)
