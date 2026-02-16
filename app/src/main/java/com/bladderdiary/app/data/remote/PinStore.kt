package com.bladderdiary.app.data.remote

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.pinDataStore by preferencesDataStore(name = "pin_store")

data class PinStoredState(
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val failedAttempts: Int = 0,
    val lockedUntilEpochMs: Long? = null
)

interface PinStoreDataSource {
    fun observe(userId: String): Flow<PinStoredState>
    suspend fun read(userId: String): PinStoredState
    suspend fun savePin(userId: String, pinHash: String, pinSalt: String)
    suspend fun updateFailedAttempts(userId: String, failedAttempts: Int, lockedUntilEpochMs: Long?)
    suspend fun clearFailedAttempts(userId: String)
    suspend fun clearUser(userId: String)
}

class PinStore(
    private val context: Context
) : PinStoreDataSource {
    override fun observe(userId: String): Flow<PinStoredState> = context.pinDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs -> prefs.toPinStoredState(userId) }

    override suspend fun read(userId: String): PinStoredState {
        return context.pinDataStore.data.first().toPinStoredState(userId)
    }

    override suspend fun savePin(userId: String, pinHash: String, pinSalt: String) {
        context.pinDataStore.edit { prefs ->
            prefs[pinHashKey(userId)] = pinHash
            prefs[pinSaltKey(userId)] = pinSalt
            prefs[failedAttemptsKey(userId)] = 0
            prefs.remove(lockedUntilKey(userId))
        }
    }

    override suspend fun updateFailedAttempts(userId: String, failedAttempts: Int, lockedUntilEpochMs: Long?) {
        context.pinDataStore.edit { prefs ->
            prefs[failedAttemptsKey(userId)] = failedAttempts
            if (lockedUntilEpochMs == null) {
                prefs.remove(lockedUntilKey(userId))
            } else {
                prefs[lockedUntilKey(userId)] = lockedUntilEpochMs
            }
        }
    }

    override suspend fun clearFailedAttempts(userId: String) {
        context.pinDataStore.edit { prefs ->
            prefs[failedAttemptsKey(userId)] = 0
            prefs.remove(lockedUntilKey(userId))
        }
    }

    override suspend fun clearUser(userId: String) {
        context.pinDataStore.edit { prefs ->
            prefs.remove(pinHashKey(userId))
            prefs.remove(pinSaltKey(userId))
            prefs.remove(failedAttemptsKey(userId))
            prefs.remove(lockedUntilKey(userId))
        }
    }

    private fun Preferences.toPinStoredState(userId: String): PinStoredState {
        return PinStoredState(
            pinHash = this[pinHashKey(userId)],
            pinSalt = this[pinSaltKey(userId)],
            failedAttempts = this[failedAttemptsKey(userId)] ?: 0,
            lockedUntilEpochMs = this[lockedUntilKey(userId)]
        )
    }

    private fun pinHashKey(userId: String) = stringPreferencesKey("pin_hash_$userId")
    private fun pinSaltKey(userId: String) = stringPreferencesKey("pin_salt_$userId")
    private fun failedAttemptsKey(userId: String) = intPreferencesKey("failed_attempts_$userId")
    private fun lockedUntilKey(userId: String) = longPreferencesKey("locked_until_$userId")
}
