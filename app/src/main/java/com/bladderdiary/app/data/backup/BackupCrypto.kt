package com.bladderdiary.app.data.backup

import com.bladderdiary.app.data.security.MemoCrypto
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupCrypto {
    private val secureRandom = SecureRandom()
    private val base64UrlEncoder = Base64.getUrlEncoder().withoutPadding()
    private val base64UrlDecoder = Base64.getUrlDecoder()

    fun generateBackupDek(): ByteArray = randomBytes(KEY_SIZE_BYTES)

    fun createPasswordEnvelope(
        dekBytes: ByteArray,
        passphrase: CharArray,
        userId: String
    ): BackupEncryptionManifest {
        val derived = deriveKek(passphrase)
        return BackupEncryptionManifest(
            kdfSalt = derived.saltBase64,
            kdfParams = BackupKdfParams(
                iterations = derived.iterations,
                keyLengthBits = derived.keyLengthBits
            ),
            wrappedBackupDek = encryptBytes(
                plainBytes = dekBytes,
                keyBytes = derived.keyBytes,
                aad = buildDekWrapAad(userId)
            )
        )
    }

    fun unwrapBackupDek(
        encryption: BackupEncryptionManifest,
        passphrase: CharArray,
        userId: String
    ): ByteArray = runCatching {
        require(encryption.scheme == BackupEncryptionManifest.BACKUP_ENCRYPTION_SCHEME) {
            "지원하지 않는 백업 암호화 방식입니다."
        }
        require(encryption.kdf == BackupEncryptionManifest.BACKUP_KDF) {
            "지원하지 않는 백업 KDF입니다."
        }
        val derived = deriveKek(
            passphrase = passphrase,
            saltBase64 = encryption.kdfSalt,
            iterations = encryption.kdfParams.iterations,
            keyLengthBits = encryption.kdfParams.keyLengthBits
        )
        decryptBytes(
            encryptedValue = encryption.wrappedBackupDek,
            keyBytes = derived.keyBytes,
            aad = buildDekWrapAad(userId)
        )
    }.getOrElse { throw BackupCryptoException(it) }

    fun encryptPayload(
        payload: BackupPlainPayloadV1,
        dekBytes: ByteArray,
        payloadId: String,
        createdAtEpochMs: Long
    ): BackupEncryptedValue {
        val plainBytes = BackupJson.encodeToString(payload).toByteArray(Charsets.UTF_8)
        return encryptBytes(
            plainBytes = plainBytes,
            keyBytes = dekBytes,
            aad = buildPayloadAad(
                userId = payload.userId,
                payloadId = payloadId,
                createdAtEpochMs = createdAtEpochMs
            )
        )
    }

    fun decryptPayload(
        encryptedValue: BackupEncryptedValue,
        dekBytes: ByteArray,
        userId: String,
        payloadId: String,
        createdAtEpochMs: Long
    ): BackupPlainPayloadV1 = runCatching {
        val plainBytes = decryptBytes(
            encryptedValue = encryptedValue,
            keyBytes = dekBytes,
            aad = buildPayloadAad(
                userId = userId,
                payloadId = payloadId,
                createdAtEpochMs = createdAtEpochMs
            )
        )
        BackupJson.decodeFromString<BackupPlainPayloadV1>(
            plainBytes.toString(Charsets.UTF_8)
        )
    }.getOrElse { throw BackupCryptoException(it) }

    private fun deriveKek(
        passphrase: CharArray,
        saltBase64: String = randomBase64Url(SALT_BYTES),
        iterations: Int = MemoCrypto.DEFAULT_PBKDF2_ITERATIONS,
        keyLengthBits: Int = MemoCrypto.DEFAULT_PBKDF2_KEY_LENGTH_BITS
    ): DerivedBackupKek {
        require(passphrase.isNotEmpty()) { "백업 비밀번호를 입력해주세요." }
        val salt = decodeBase64Url(saltBase64)
        val keySpec = PBEKeySpec(passphrase, salt, iterations, keyLengthBits)
        val keyFactory = SecretKeyFactory.getInstance(BackupEncryptionManifest.BACKUP_KDF)
        val keyBytes = keyFactory.generateSecret(keySpec).encoded
        keySpec.clearPassword()
        return DerivedBackupKek(
            saltBase64 = saltBase64,
            iterations = iterations,
            keyLengthBits = keyLengthBits,
            keyBytes = keyBytes
        )
    }

    private fun encryptBytes(
        plainBytes: ByteArray,
        keyBytes: ByteArray,
        aad: String
    ): BackupEncryptedValue {
        val nonce = randomBytes(GCM_NONCE_BYTES)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val key = SecretKeySpec(keyBytes, AES_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, nonce))
        cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
        val encrypted = cipher.doFinal(plainBytes)
        val cipherText = encrypted.copyOfRange(0, encrypted.size - GCM_TAG_BYTES)
        val tag = encrypted.copyOfRange(encrypted.size - GCM_TAG_BYTES, encrypted.size)
        return BackupEncryptedValue(
            nonce = encodeBase64Url(nonce),
            ciphertext = encodeBase64Url(cipherText),
            tag = encodeBase64Url(tag)
        )
    }

    private fun decryptBytes(
        encryptedValue: BackupEncryptedValue,
        keyBytes: ByteArray,
        aad: String
    ): ByteArray {
        val nonce = decodeBase64Url(encryptedValue.nonce)
        val cipherText = decodeBase64Url(encryptedValue.ciphertext)
        val tag = decodeBase64Url(encryptedValue.tag)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val key = SecretKeySpec(keyBytes, AES_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, nonce))
        cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(cipherText + tag)
    }

    private fun buildDekWrapAad(userId: String): String =
        "$BACKUP_AAD_PREFIX|dek-wrap|${BackupEnvelopeV1.BACKUP_VERSION}|$userId"

    private fun buildPayloadAad(userId: String, payloadId: String, createdAtEpochMs: Long): String =
        "$BACKUP_AAD_PREFIX|payload|${BackupEnvelopeV1.BACKUP_VERSION}|" +
            "$userId|$payloadId|$createdAtEpochMs"

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).also(secureRandom::nextBytes)

    private fun randomBase64Url(size: Int): String = encodeBase64Url(randomBytes(size))

    private fun encodeBase64Url(value: ByteArray): String = base64UrlEncoder.encodeToString(value)

    private fun decodeBase64Url(value: String): ByteArray = base64UrlDecoder.decode(value)

    private data class DerivedBackupKek(
        val saltBase64: String,
        val iterations: Int,
        val keyLengthBits: Int,
        val keyBytes: ByteArray
    )

    private companion object {
        private const val KEY_SIZE_BYTES = 32
        private const val SALT_BYTES = 16
        private const val GCM_NONCE_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AES_ALGORITHM = "AES"
        private const val BACKUP_AAD_PREFIX = "com.chausoft.bladderdiary.backup"
    }
}
