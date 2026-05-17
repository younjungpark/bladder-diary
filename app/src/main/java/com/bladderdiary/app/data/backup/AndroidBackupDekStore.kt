package com.bladderdiary.app.data.backup

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.IOException
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.backupDekStoreDataStore by preferencesDataStore(
    name = "backup_dek_store"
)

class AndroidBackupDekStore(private val context: Context) : BackupDekStore {
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }
    private val base64Encoder = Base64.getUrlEncoder().withoutPadding()
    private val base64Decoder = Base64.getUrlDecoder()

    override suspend fun save(userId: String, storedBackupDek: StoredBackupDek) {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey(userId))
        val locallyWrappedDek = buildString {
            append(base64Encoder.encodeToString(cipher.iv))
            append(DELIMITER)
            append(base64Encoder.encodeToString(cipher.doFinal(storedBackupDek.dekBytes)))
        }
        context.backupDekStoreDataStore.edit { prefs ->
            prefs[localWrappedDekKey(userId)] = locallyWrappedDek
            prefs[passwordEnvelopeKey(userId)] = BackupJson.encodeToString(
                storedBackupDek.passwordEnvelope
            )
        }
    }

    override suspend fun load(userId: String): StoredBackupDek? {
        val snapshot = context.backupDekStoreDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { prefs ->
                prefs[localWrappedDekKey(userId)] to prefs[passwordEnvelopeKey(userId)]
            }
            .first()
        val locallyWrappedDek = snapshot.first ?: return null
        val passwordEnvelopeJson = snapshot.second ?: return null

        return runCatching {
            val parts = locallyWrappedDek.split(DELIMITER, limit = 2)
            require(parts.size == 2) { "로컬 백업 키 형식이 올바르지 않습니다." }
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(userId),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, base64Decoder.decode(parts[0]))
            )
            StoredBackupDek(
                dekBytes = cipher.doFinal(base64Decoder.decode(parts[1])),
                passwordEnvelope = BackupJson.decodeFromString(passwordEnvelopeJson)
            )
        }.getOrElse {
            clear(userId)
            null
        }
    }

    override suspend fun clear(userId: String) {
        context.backupDekStoreDataStore.edit { prefs ->
            prefs.remove(localWrappedDekKey(userId))
            prefs.remove(passwordEnvelopeKey(userId))
        }
        if (keyStore.containsAlias(keyAlias(userId))) {
            keyStore.deleteEntry(keyAlias(userId))
        }
    }

    private fun getOrCreateSecretKey(userId: String): SecretKey {
        val existingKey = keyStore.getKey(keyAlias(userId), null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            keyAlias(userId),
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun keyAlias(userId: String): String = "bladder_diary_backup_dek_$userId"

    private fun localWrappedDekKey(userId: String) = stringPreferencesKey(
        "backup_local_wrapped_dek_$userId"
    )

    private fun passwordEnvelopeKey(userId: String) = stringPreferencesKey(
        "backup_password_envelope_$userId"
    )

    private companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val DELIMITER = "."
    }
}
