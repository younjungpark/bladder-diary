package com.bladderdiary.app.data.backup

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bladderdiary.app.domain.model.BackupSettingsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.backupPreferenceDataStore by preferencesDataStore(
    name = "backup_preference_store"
)

class BackupPreferenceStore(private val context: Context) {
    fun observe(userId: String): Flow<BackupSettingsState> = context.backupPreferenceDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs -> prefs.toBackupSettingsState(userId) }

    suspend fun read(userId: String): BackupSettingsState =
        context.backupPreferenceDataStore.data.first().toBackupSettingsState(userId)

    suspend fun setAutoBackupEnabled(userId: String, isEnabled: Boolean) {
        context.backupPreferenceDataStore.edit { prefs ->
            prefs[autoEnabledKey(userId)] = isEnabled
        }
    }

    suspend fun markBackupSuccess(userId: String, completedAtEpochMs: Long) {
        context.backupPreferenceDataStore.edit { prefs ->
            prefs[connectedKey(userId)] = true
            prefs[lastSuccessKey(userId)] = completedAtEpochMs
            prefs.remove(lastFailureKey(userId))
            prefs.remove(lastErrorKey(userId))
        }
    }

    suspend fun markBackupFailure(userId: String, failedAtEpochMs: Long, message: String?) {
        context.backupPreferenceDataStore.edit { prefs ->
            prefs[lastFailureKey(userId)] = failedAtEpochMs
            prefs[lastErrorKey(userId)] = message?.take(MAX_ERROR_LENGTH).orEmpty()
        }
    }

    suspend fun clearUser(userId: String) {
        context.backupPreferenceDataStore.edit { prefs ->
            prefs.remove(connectedKey(userId))
            prefs.remove(autoEnabledKey(userId))
            prefs.remove(lastSuccessKey(userId))
            prefs.remove(lastFailureKey(userId))
            prefs.remove(lastErrorKey(userId))
        }
    }

    private fun Preferences.toBackupSettingsState(userId: String): BackupSettingsState =
        BackupSettingsState(
            isDriveBackupConnected = this[connectedKey(userId)] ?: false,
            isAutoBackupEnabled = this[autoEnabledKey(userId)] ?: false,
            lastBackupSuccessEpochMs = this[lastSuccessKey(userId)],
            lastBackupFailureEpochMs = this[lastFailureKey(userId)],
            lastBackupErrorMessage = this[lastErrorKey(userId)]?.takeIf { it.isNotBlank() }
        )

    private fun connectedKey(userId: String) = booleanPreferencesKey("drive_connected_$userId")

    private fun autoEnabledKey(userId: String) = booleanPreferencesKey("auto_enabled_$userId")

    private fun lastSuccessKey(userId: String) = longPreferencesKey("last_success_$userId")

    private fun lastFailureKey(userId: String) = longPreferencesKey("last_failure_$userId")

    private fun lastErrorKey(userId: String) = stringPreferencesKey("last_error_$userId")

    private companion object {
        private const val MAX_ERROR_LENGTH = 240
    }
}
