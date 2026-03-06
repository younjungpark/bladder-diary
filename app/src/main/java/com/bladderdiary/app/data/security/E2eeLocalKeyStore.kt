package com.bladderdiary.app.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.e2eeLocalKeyStoreDataStore by preferencesDataStore(name = "e2ee_local_key_store")

interface E2eeLocalKeyStoreDataSource {
    suspend fun saveDek(userId: String, dekBytes: ByteArray)
    suspend fun loadDek(userId: String): ByteArray?
    suspend fun clearDek(userId: String)
}

class E2eeLocalKeyStore(
    private val context: Context
) : E2eeLocalKeyStoreDataSource {
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }
    private val base64Encoder = Base64.getUrlEncoder().withoutPadding()
    private val base64Decoder = Base64.getUrlDecoder()

    override suspend fun saveDek(userId: String, dekBytes: ByteArray) {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey(userId))
        val wrappedValue = buildString {
            append(base64Encoder.encodeToString(cipher.iv))
            append(DELIMITER)
            append(base64Encoder.encodeToString(cipher.doFinal(dekBytes)))
        }
        context.e2eeLocalKeyStoreDataStore.edit { prefs ->
            prefs[wrappedDekKey(userId)] = wrappedValue
        }
    }

    override suspend fun loadDek(userId: String): ByteArray? {
        val wrappedValue = context.e2eeLocalKeyStoreDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { prefs -> prefs[wrappedDekKey(userId)] }
            .first()
            ?: return null

        return runCatching {
            val parts = wrappedValue.split(DELIMITER, limit = 2)
            require(parts.size == 2) { "로컬 E2EE 키 형식이 올바르지 않습니다." }
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(userId),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, base64Decoder.decode(parts[0]))
            )
            cipher.doFinal(base64Decoder.decode(parts[1]))
        }.getOrElse {
            clearDek(userId)
            null
        }
    }

    override suspend fun clearDek(userId: String) {
        context.e2eeLocalKeyStoreDataStore.edit { prefs ->
            prefs.remove(wrappedDekKey(userId))
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

    private fun keyAlias(userId: String): String = "bladder_diary_e2ee_dek_$userId"

    private fun wrappedDekKey(userId: String) = stringPreferencesKey("wrapped_dek_$userId")

    private companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val DELIMITER = "."
    }
}
