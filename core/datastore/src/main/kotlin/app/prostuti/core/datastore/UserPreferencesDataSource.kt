package app.prostuti.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPrefs: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

data class UserPreferences(
    val languageTag: String?,
    val theme: String,
    val dataSaver: Boolean,
    val downloadOnWifiOnly: Boolean,
    val studyReminderHour: Int?,
    val userId: String?,
    val onboardingCompleted: Boolean,
    val lastSyncedAtMillis: Long?,
)

@Singleton
class UserPreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val preferences: Flow<UserPreferences> = context.userPrefs.data.map { p ->
        UserPreferences(
            languageTag = p[LANGUAGE],
            theme = p[THEME] ?: "SYSTEM",
            dataSaver = p[DATA_SAVER] ?: false,
            downloadOnWifiOnly = p[WIFI_ONLY] ?: true,
            studyReminderHour = p[REMINDER_HOUR]?.takeIf { it >= 0 },
            userId = p[USER_ID],
            onboardingCompleted = p[ONBOARDED] ?: false,
            lastSyncedAtMillis = p[LAST_SYNC],
        )
    }

    suspend fun setLanguage(tag: String) = context.userPrefs.edit { it[LANGUAGE] = tag }
    suspend fun setTheme(theme: String) = context.userPrefs.edit { it[THEME] = theme }
    suspend fun setDataSaver(enabled: Boolean) = context.userPrefs.edit { it[DATA_SAVER] = enabled }
    suspend fun setDownloadOnWifiOnly(enabled: Boolean) = context.userPrefs.edit { it[WIFI_ONLY] = enabled }
    suspend fun setStudyReminderHour(hour: Int?) = context.userPrefs.edit { it[REMINDER_HOUR] = hour ?: -1 }
    suspend fun setLastSyncedAt(millis: Long) = context.userPrefs.edit { it[LAST_SYNC] = millis }

    suspend fun setSession(userId: String?, onboardingCompleted: Boolean) = context.userPrefs.edit {
        if (userId == null) it.remove(USER_ID) else it[USER_ID] = userId
        it[ONBOARDED] = onboardingCompleted
    }

    private companion object {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme")
        val DATA_SAVER = booleanPreferencesKey("data_saver")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val USER_ID = stringPreferencesKey("user_id")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val LAST_SYNC = longPreferencesKey("last_sync")
    }
}
