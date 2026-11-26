package app.prostuti.feature.offline

import android.text.format.Formatter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.prostuti.core.designsystem.component.*
import app.prostuti.core.designsystem.theme.ProstutiThemeExt
import app.prostuti.core.designsystem.theme.Spacing
import app.prostuti.core.domain.repository.OfflineRepository
import app.prostuti.core.domain.repository.SettingsRepository
import app.prostuti.core.model.DownloadKind
import app.prostuti.core.model.DownloadState
import app.prostuti.core.ui.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@Serializable data object DownloadsRoute

fun NavGraphBuilder.offlineGraph(@Suppress("UNUSED_PARAMETER") navController: NavController) {
    composable<DownloadsRoute> { DownloadsScreen() }
}

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val offline: OfflineRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    val downloads = offline.downloads().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val storage = offline.storage().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val sync = offline.syncStatus().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val appSettings = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun remove(id: String) = viewModelScope.launch { offline.remove(id) }
    fun clearCache() = viewModelScope.launch { offline.clearCache() }
    fun syncNow() = offline.requestSync()
    fun setWifiOnly(v: Boolean) = viewModelScope.launch { settings.setDownloadOnWifiOnly(v) }
    fun setDataSaver(v: Boolean) = viewModelScope.launch { settings.setDataSaver(v) }
}

@Composable
internal fun DownloadsScreen(viewModel: DownloadsViewModel = hiltViewModel()) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val storage by viewModel.storage.collectAsStateWithLifecycle()
    val sync by viewModel.sync.collectAsStateWithLifecycle()
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    val context = LocalContext.current
    fun size(bytes: Long) = Formatter.formatShortFileSize(context, bytes)
    PtScaffold(stringResource(R.string.of_title), onBack = { nav.navigate(Destination.Back) }) { padding ->
        ReadableWidth {
            LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                storage?.let { s ->
                    item {
                        PtCard(Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.of_storage), style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(Spacing.sm))
                            val total = (s.downloadsBytes + s.cacheBytes + s.freeDeviceBytes).coerceAtLeast(1).toFloat()
                            SegmentedMeter(listOf(
                                s.downloadsBytes / total to MaterialTheme.colorScheme.primary,
                                s.cacheBytes / total to ProstutiThemeExt.colors.community,
                                s.freeDeviceBytes / total to ProstutiThemeExt.colors.chartTrack,
                            ).map { (v, c) -> v.coerceAtLeast(0.01f) to c })
                            Spacer(Modifier.height(Spacing.sm))
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Dot(MaterialTheme.colorScheme.primary); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.of_downloads, size(s.downloadsBytes)), style = MaterialTheme.typography.labelMedium) }
                                Row(verticalAlignment = Alignment.CenterVertically) { Dot(ProstutiThemeExt.colors.community); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.of_cache, size(s.cacheBytes)), style = MaterialTheme.typography.labelMedium) }
                            }
                            Text(stringResource(R.string.of_free, size(s.freeDeviceBytes)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            PtButton(stringResource(R.string.of_clear_cache), viewModel::clearCache, style = PtButtonStyle.Text, leadingIcon = Icons.Rounded.CleaningServices)
                        }
                    }
                }
                sync?.let { s ->
                    item {
                        PtCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconBadge(if (s.pendingChanges == 0) Icons.Rounded.CloudDone else Icons.Rounded.CloudSync, size = 36.dp)
                                Spacer(Modifier.width(Spacing.md))
                                Column(Modifier.weight(1f)) {
                                    Text(if (s.pendingChanges == 0) stringResource(R.string.of_all_synced) else stringResource(R.string.of_pending, s.pendingChanges.loc()), style = MaterialTheme.typography.titleSmall)
                                    s.lastSyncedAt?.let {
                                        Text(stringResource(R.string.of_last_synced, it.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM, HH:mm")).loc()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                PtButton(stringResource(if (s.isSyncing) R.string.of_syncing else R.string.of_sync_now), viewModel::syncNow, style = PtButtonStyle.Tonal, loading = s.isSyncing)
                            }
                        }
                    }
                }
                settings?.let { st ->
                    item {
                        PtCard(Modifier.fillMaxWidth()) {
                            SettingSwitch(stringResource(R.string.of_wifi_only), stringResource(R.string.of_wifi_only_desc), st.downloadOnWifiOnly, viewModel::setWifiOnly)
                            HairlineDivider()
                            SettingSwitch(stringResource(R.string.of_data_saver), stringResource(R.string.of_data_saver_desc), st.dataSaver, viewModel::setDataSaver)
                        }
                    }
                }
                item { SectionHeader(stringResource(R.string.of_items)) }
                val list = downloads
                if (list != null && list.isEmpty()) item { EmptyState(Icons.Rounded.CloudDownload, stringResource(R.string.of_empty_title), stringResource(R.string.of_empty_body)) }
                items(list.orEmpty(), key = { it.id }) { d ->
                    PtCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                        ListRow(
                            d.title.text(),
                            subtitle = when (d.state) {
                                DownloadState.COMPLETED -> size(d.sizeBytes)
                                DownloadState.QUEUED -> stringResource(R.string.of_state_queued)
                                DownloadState.DOWNLOADING -> stringResource(R.string.of_state_downloading, percent(d.progress))
                                DownloadState.FAILED -> stringResource(R.string.of_state_failed)
                            },
                            leading = {
                                IconBadge(when (d.kind) {
                                    DownloadKind.MATERIAL -> Icons.AutoMirrored.Rounded.Article
                                    DownloadKind.QUESTION_SET -> Icons.Rounded.Quiz
                                    DownloadKind.FLASHCARD_DECK -> Icons.Rounded.Style
                                }, size = 36.dp)
                            },
                            trailing = { IconButton(onClick = { viewModel.remove(d.id) }) { Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.of_remove)) } },
                        )
                        if (d.state == DownloadState.DOWNLOADING) MeterBar(d.progress, Modifier.padding(horizontal = Spacing.lg).padding(bottom = Spacing.md), height = 4.dp)
                    }
                }
                item {
                    PtCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, border = false) {
                        Text(stringResource(R.string.of_what_works), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.of_works_list), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(Spacing.sm))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Rounded.Timer, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.of_mock_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onChange)
    }
}
