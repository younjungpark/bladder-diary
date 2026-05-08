package com.bladderdiary.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bladderdiary.app.domain.model.CloudSyncPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.cloudSyncPreferenceDataStore by preferencesDataStore(
    name = "cloud_sync_preference_store"
)

class CloudSyncPreferenceStore(private val context: Context) {
    fun observe(userId: String): Flow<CloudSyncPreference> =
        context.cloudSyncPreferenceDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { prefs -> prefs.toCloudSyncPreference(userId) }

    suspend fun read(userId: String): CloudSyncPreference =
        context.cloudSyncPreferenceDataStore.data.first().toCloudSyncPreference(userId)

    suspend fun setEnabled(userId: String, isEnabled: Boolean) {
        context.cloudSyncPreferenceDataStore.edit { prefs ->
            prefs[enabledKey(userId)] = isEnabled
            prefs[choiceVersionKey(userId)] = CURRENT_CHOICE_VERSION
        }
    }

    suspend fun clearUser(userId: String) {
        context.cloudSyncPreferenceDataStore.edit { prefs ->
            prefs.remove(enabledKey(userId))
            prefs.remove(choiceVersionKey(userId))
        }
    }

    private fun Preferences.toCloudSyncPreference(userId: String): CloudSyncPreference =
        CloudSyncPreference(
            isEnabled = this[enabledKey(userId)] ?: false,
            hasUserChoice = (this[choiceVersionKey(userId)] ?: 0) >= CURRENT_CHOICE_VERSION
        )

    private fun enabledKey(userId: String) = booleanPreferencesKey("cloud_sync_enabled_$userId")

    private fun choiceVersionKey(userId: String) =
        intPreferencesKey("cloud_sync_choice_version_$userId")

    private companion object {
        private const val CURRENT_CHOICE_VERSION = 1
    }
}
