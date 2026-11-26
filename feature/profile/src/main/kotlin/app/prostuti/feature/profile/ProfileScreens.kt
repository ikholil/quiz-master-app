package app.prostuti.feature.profile

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.prostuti.core.designsystem.component.*
import app.prostuti.core.designsystem.theme.ProstutiThemeExt
import app.prostuti.core.designsystem.theme.Spacing
import app.prostuti.core.domain.repository.ThemePreference
import app.prostuti.core.model.*
import app.prostuti.core.ui.*
import kotlinx.coroutines.launch
import java.time.ZoneId

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileScreen(onPrivacy: () -> Unit, onSupport: () -> Unit, onAccountData: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val sub by viewModel.subscription.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        ReadableWidth {
            LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(vertical = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                item {
                    Row(Modifier.screenGutter(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                            Text(profile?.fullName?.take(1)?.uppercase().orEmpty(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(Modifier.width(Spacing.lg))
                        Column(Modifier.weight(1f)) {
                            Text(profile?.fullName.orEmpty(), style = MaterialTheme.typography.titleLarge)
                            Text((profile?.phone ?: profile?.email).orEmpty().loc(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            profile?.let { Text(stringResource(R.string.pf_daily, minutesLabel(it.dailyStudyMinutes)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
                profile?.targets?.takeIf { it.isNotEmpty() }?.let { targets ->
                    item {
                        Column(Modifier.screenGutter()) {
                            Text(stringResource(R.string.pf_targets), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(Spacing.sm))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                targets.forEach { t ->
                                    Pill(t.examName.text() + (t.examDate?.let { " · " + it.pretty("d MMM") } ?: ""),
                                        color = if (t.isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        container = if (t.isPrimary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        icon = if (t.isPrimary) Icons.Rounded.Star else null)
                                }
                            }
                            PtButton(stringResource(R.string.pf_edit_prefs), { nav.navigate(Destination.EditPreferences) }, style = PtButtonStyle.Text, leadingIcon = Icons.Rounded.Tune)
                        }
                    }
                }
                item {
                    PtCard(Modifier.fillMaxWidth().screenGutter(), onClick = { nav.navigate(Destination.Subscription) },
                        color = if (sub.isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else ProstutiThemeExt.colors.communityContainer.copy(alpha = 0.6f), border = false) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.WorkspacePremium, null, tint = if (sub.isActive) MaterialTheme.colorScheme.primary else ProstutiThemeExt.colors.community)
                            Spacer(Modifier.width(Spacing.md))
                            Text(
                                if (sub.isActive) stringResource(R.string.pf_plus_active, sub.planName?.text().orEmpty(), sub.expiresAt?.pretty().orEmpty())
                                else stringResource(R.string.pf_free) + " · " + stringResource(R.string.pf_upgrade),
                                style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f),
                            )
                            Icon(Icons.Rounded.ChevronRight, null)
                        }
                    }
                }
                if (achievements.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.pf_achievements), modifier = Modifier.screenGutter()) }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = Spacing.gutter), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            items(achievements, key = { it.id }) { a ->
                                val unlocked = a.unlockedAt != null
                                PtCard(Modifier.width(150.dp)) {
                                    IconBadge(Icons.Rounded.EmojiEvents, Modifier.alpha(if (unlocked) 1f else 0.45f),
                                        tint = if (unlocked) ProstutiThemeExt.colors.streak else MaterialTheme.colorScheme.outline,
                                        container = if (unlocked) ProstutiThemeExt.colors.communityContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    Spacer(Modifier.height(Spacing.sm))
                                    Text(a.title.text(), style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                    Text(a.description.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, minLines = 2)
                                    if (!unlocked) { Spacer(Modifier.height(6.dp)); MeterBar(a.progress, height = 4.dp) }
                                }
                            }
                        }
                    }
                }
                item { MenuGroup(stringResource(R.string.pf_menu_progress), listOf(
                    MenuItem(Icons.Rounded.Insights, stringResource(R.string.pf_analytics)) { nav.navigate(Destination.Analytics) },
                    MenuItem(Icons.Rounded.History, stringResource(R.string.pf_mock_history)) { nav.navigate(Destination.MockHistory) },
                    MenuItem(Icons.Rounded.CalendarMonth, stringResource(R.string.pf_planner)) { nav.navigate(Destination.Planner) },
                )) }
                item { MenuGroup(stringResource(R.string.pf_menu_tools), listOf(
                    MenuItem(Icons.Rounded.AccountBalance, stringResource(R.string.pf_exams)) { nav.navigate(Destination.ExamDirectory) },
                    MenuItem(Icons.Rounded.CloudDownload, stringResource(R.string.pf_downloads)) { nav.navigate(Destination.Downloads) },
                    MenuItem(Icons.Rounded.Forum, stringResource(R.string.pf_community)) { nav.navigate(Destination.Community) },
                    MenuItem(Icons.Rounded.AutoAwesome, stringResource(R.string.pf_tutor)) { nav.navigate(Destination.Assistant()) },
                )) }
                item { MenuGroup(stringResource(R.string.pf_menu_account), listOf(
                    MenuItem(Icons.Rounded.Notifications, stringResource(R.string.pf_notifications)) { nav.navigate(Destination.Notifications) },
                    MenuItem(Icons.Rounded.Settings, stringResource(R.string.pf_settings)) { nav.navigate(Destination.Settings) },
                    MenuItem(Icons.Rounded.Shield, stringResource(R.string.pf_privacy), onPrivacy),
                    MenuItem(Icons.Rounded.SupportAgent, stringResource(R.string.pf_support), onSupport),
                    MenuItem(Icons.Rounded.Folder, stringResource(R.string.pf_account_data), onAccountData),
                )) }
                item {
                    PtButton(stringResource(R.string.pf_sign_out), { confirmSignOut = true }, Modifier.screenGutter(), style = PtButtonStyle.Text, leadingIcon = Icons.AutoMirrored.Rounded.Logout)
                    Text(stringResource(R.string.pf_version, LocalBuildInfo.current.versionName), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.screenGutter())
                }
            }
        }
    }
    if (confirmSignOut) AlertDialog(
        onDismissRequest = { confirmSignOut = false },
        text = { Text(stringResource(R.string.pf_sign_out_confirm)) },
        confirmButton = { TextButton({ confirmSignOut = false; viewModel.signOut() }) { Text(stringResource(R.string.pf_sign_out)) } },
        dismissButton = { TextButton({ confirmSignOut = false }) { Text(stringResource(R.string.pf_cancel)) } },
    )
}

private data class MenuItem(val icon: ImageVector, val title: String, val onClick: () -> Unit)

@Composable
private fun MenuGroup(title: String, items: List<MenuItem>) {
    Column(Modifier.screenGutter()) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = Spacing.sm))
        PtCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = Spacing.xs)) {
            items.forEachIndexed { i, m ->
                if (i > 0) HairlineDivider(inset = 60.dp)
                ListRow(m.title, leading = { Icon(m.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp)) }, showChevron = true, onClick = m.onClick)
            }
        }
    }
}

@Composable
internal fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    var pendingHour by remember { mutableStateOf<Int?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.setReminder(pendingHour)
    }
    PtScaffold(stringResource(R.string.st_title), onBack = { nav.navigate(Destination.Back) }) { padding ->
        val settings = s ?: return@PtScaffold
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
            Column {
                Text(stringResource(R.string.st_language), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(Spacing.sm))
                SegmentedControl(listOf(AppLanguage.BENGALI, AppLanguage.ENGLISH), settings.language, viewModel::setLanguage, label = { if (it == AppLanguage.BENGALI) "বাংলা" else "English" })
            }
            Column {
                Text(stringResource(R.string.st_theme), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(Spacing.sm))
                SegmentedControl(ThemePreference.entries, settings.theme, viewModel::setTheme, label = {
                    stringResource(when (it) { ThemePreference.SYSTEM -> R.string.st_theme_system; ThemePreference.LIGHT -> R.string.st_theme_light; ThemePreference.DARK -> R.string.st_theme_dark })
                })
            }
            Column {
                Text(stringResource(R.string.st_reminder), style = MaterialTheme.typography.titleSmall)
                Text(settings.studyReminderHour?.let { stringResource(R.string.st_reminder_at, "%02d:00".format(it).loc()) } ?: stringResource(R.string.st_reminder_off),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.sm))
                SegmentedControl(listOf<Int?>(null, 7, 16, 20, 22), settings.studyReminderHour, { h ->
                    if (h != null && Build.VERSION.SDK_INT >= 33) { pendingHour = h; permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    else viewModel.setReminder(h)
                }, label = { it?.let { h -> "%02d".format(h).loc() } ?: stringResource(R.string.st_reminder_off) })
            }
            ListRow(stringResource(R.string.st_data), leading = { Icon(Icons.Rounded.Storage, null, tint = MaterialTheme.colorScheme.primary) }, showChevron = true, onClick = { nav.navigate(Destination.Downloads) })
        }
    }
}

@Composable
internal fun PrivacyScreen(viewModel: PrivacyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    PtScaffold(stringResource(R.string.pv_title), onBack = { nav.navigate(Destination.Back) }) { padding ->
        StateContent(state, onRetry = viewModel::load, modifier = Modifier.padding(padding)) { p ->
            Column(Modifier.padding(padding).padding(Spacing.gutter)) {
                PtCard(Modifier.fillMaxWidth()) {
                    Toggle(stringResource(R.string.pv_leaderboards), stringResource(R.string.pv_leaderboards_desc), p.showOnLeaderboards) { v -> viewModel.change { it.copy(showOnLeaderboards = v) } }
                    HairlineDivider()
                    Toggle(stringResource(R.string.pv_share), stringResource(R.string.pv_share_desc), p.shareProgressWithGroups) { v -> viewModel.change { it.copy(shareProgressWithGroups = v) } }
                    HairlineDivider()
                    Toggle(stringResource(R.string.pv_personal), stringResource(R.string.pv_personal_desc), p.personalizedRecommendations) { v -> viewModel.change { it.copy(personalizedRecommendations = v) } }
                    HairlineDivider()
                    Toggle(stringResource(R.string.pv_marketing), stringResource(R.string.pv_marketing_desc), p.marketingNotifications) { v -> viewModel.change { it.copy(marketingNotifications = v) } }
                }
            }
        }
    }
}

@Composable
private fun Toggle(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = Spacing.md), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = Spacing.md)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onChange)
    }
}

@Composable
internal fun SubscriptionScreen(viewModel: SubscriptionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val checkingOut by viewModel.checkingOut.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    MessagesEffect(viewModel.messages, snackbar)
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    PtScaffold(stringResource(R.string.sb_title), onBack = { nav.navigate(Destination.Back) }, snackbarHostState = snackbar) { padding ->
        StateContent(state, onRetry = viewModel::load, modifier = Modifier.padding(padding)) { plans ->
            val choice = selected ?: plans.firstOrNull { it.isPopular }?.id ?: plans.firstOrNull()?.id
            Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.gutter)) {
                Text(stringResource(R.string.sb_subtitle), style = MaterialTheme.typography.titleMedium)
                if (status.isActive) Text(stringResource(R.string.sb_active, status.planName?.text().orEmpty()), color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(Spacing.lg))
                listOf(
                    Icons.Rounded.Timer to R.string.sb_full_mocks, Icons.Rounded.AutoAwesome to R.string.sb_unlimited_ai,
                    Icons.Rounded.CloudDownload to R.string.sb_offline, Icons.Rounded.PlayCircle to R.string.sb_video, Icons.Rounded.Insights to R.string.sb_analytics,
                ).forEach { (icon, label) ->
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(Spacing.md))
                        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(Modifier.height(Spacing.xl))
                plans.forEach { p ->
                    val isSel = p.id == choice
                    PtCard(
                        Modifier.fillMaxWidth().padding(bottom = Spacing.sm).selectable(isSel, role = Role.RadioButton) { selected = p.id },
                        color = if (isSel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(isSel, null)
                            Spacer(Modifier.width(Spacing.sm))
                            Column(Modifier.weight(1f)) {
                                Text(p.name.text(), style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(R.string.sb_per_month, (p.priceBdt / p.periodMonths).loc()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                if (p.isPopular) Pill(stringResource(R.string.sb_popular), color = MaterialTheme.colorScheme.onPrimary, container = MaterialTheme.colorScheme.primary)
                                Text(stringResource(R.string.sb_price, p.priceBdt.loc()), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
                PtButton(stringResource(R.string.sb_continue), { choice?.let { id -> viewModel.checkout(id) { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } } }, Modifier.fillMaxWidth(), loading = checkingOut, enabled = choice != null)
                Spacer(Modifier.height(Spacing.sm))
                Text(stringResource(R.string.sb_payment_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun SupportScreen(viewModel: SupportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var subject by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    val sent = stringResource(R.string.sp_sent)
    MessagesEffect(viewModel.messages, snackbar)
    PtScaffold(stringResource(R.string.sp_title), onBack = { nav.navigate(Destination.Back) }, snackbarHostState = snackbar) { padding ->
        StateContent(state, onRetry = viewModel::load, modifier = Modifier.padding(padding)) { tickets ->
            LazyColumn(Modifier.padding(padding).imePadding(), contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                item {
                    PtCard(Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.sp_new), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(Spacing.sm))
                        PtTextField(subject, { subject = it }, stringResource(R.string.sp_subject))
                        Spacer(Modifier.height(Spacing.sm))
                        PtTextField(message, { message = it }, stringResource(R.string.sp_message), singleLine = false, minLines = 3)
                        Spacer(Modifier.height(Spacing.sm))
                        PtButton(stringResource(R.string.sp_send), { viewModel.create(subject.trim(), message.trim()) { subject = ""; message = ""; scope.launch { snackbar.showSnackbar(sent) } } },
                            enabled = subject.isNotBlank() && message.isNotBlank())
                    }
                }
                if (tickets.isEmpty()) item { Text(stringResource(R.string.sp_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(tickets, key = { it.id }) { t ->
                    val (label, color) = when (t.status) {
                        TicketStatus.OPEN -> R.string.sp_open to ProstutiThemeExt.colors.community
                        TicketStatus.IN_PROGRESS -> R.string.sp_in_progress to ProstutiThemeExt.colors.verified
                        TicketStatus.RESOLVED -> R.string.sp_resolved to ProstutiThemeExt.colors.correct
                    }
                    PtCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(t.subject, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Pill(stringResource(label), color = color)
                        }
                        Text(t.createdAt.atZone(ZoneId.systemDefault()).toLocalDate().pretty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        t.lastReply?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AccountDataScreen(viewModel: AccountDataViewModel = hiltViewModel()) {
    val nav = LocalNavigator.current
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var reason by rememberSaveable { mutableStateOf("") }
    val exported = stringResource(R.string.ac_export_requested)
    PtScaffold(stringResource(R.string.ac_title), onBack = { nav.navigate(Destination.Back) }, snackbarHostState = snackbar) { padding ->
        Column(Modifier.padding(padding).padding(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            PtCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.ac_export), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.ac_export_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PtButton(stringResource(R.string.ac_export), { viewModel.export { scope.launch { snackbar.showSnackbar(exported) } } }, style = PtButtonStyle.Tonal, loading = busy, leadingIcon = Icons.Rounded.Download)
            }
            PtCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.ac_delete), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.ac_delete_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PtButton(stringResource(R.string.ac_delete), { confirmDelete = true }, style = PtButtonStyle.Text, leadingIcon = Icons.Rounded.DeleteForever)
            }
            error?.let { Text(errorMessage(it), color = MaterialTheme.colorScheme.error) }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.ac_delete_title)) },
        text = {
            Column {
                Text(stringResource(R.string.ac_delete_desc))
                Spacer(Modifier.height(Spacing.md))
                PtTextField(reason, { reason = it }, stringResource(R.string.ac_delete_reason), singleLine = false)
            }
        },
        confirmButton = { TextButton({ confirmDelete = false; viewModel.delete(reason) {} }) { Text(stringResource(R.string.ac_delete_confirm), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text(stringResource(R.string.pf_cancel)) } },
    )
}

@Composable
internal fun NotificationsScreen(viewModel: NotificationsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    PtScaffold(stringResource(R.string.nf_title), onBack = { nav.navigate(Destination.Back) }) { padding ->
        StateContent(state, onRetry = viewModel::load, modifier = Modifier.padding(padding)) { list ->
            if (list.isEmpty()) EmptyState(Icons.Rounded.NotificationsNone, stringResource(R.string.nf_title), stringResource(R.string.nf_empty))
            else LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(list, key = { it.id }) { n ->
                    PtCard(Modifier.fillMaxWidth(), color = if (n.isRead) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)) {
                        Row(verticalAlignment = Alignment.Top) {
                            if (!n.isRead) { Dot(MaterialTheme.colorScheme.primary, Modifier.padding(top = 6.dp)); Spacer(Modifier.width(Spacing.sm)) }
                            Column {
                                Text(n.title.text(), style = MaterialTheme.typography.titleSmall)
                                Text(n.body.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
