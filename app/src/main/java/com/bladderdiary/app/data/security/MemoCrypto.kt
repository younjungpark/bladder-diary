package com.bladderdiary.app.data.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object MemoEncryptionScheme {
    const val NONE = "NONE"
    const val E2EE_V1 = "E2EE_V1"
}

data class MemoKeyDerivation(
    val saltBase64: String,
    val iterations: Int,
    val keyLengthBits: Int,
    val keyBytes: ByteArray
)

@Serializable
private data class MemoCipherEnvelope(
    val v: Int,
    val alg: String,
    val kid: String,
    val nonce: String,
    val ct: String,
    val tag: String
)

object MemoCrypto {
    private const val KEY_SIZE_BYTES = 32
    private const val GCM_NONCE_BYTES = 12
    private const val GCM_TAG_BITS = 128
    const val DEFAULT_PBKDF2_ITERATIONS = 210_000
    const val DEFAULT_PBKDF2_KEY_LENGTH_BITS = 256

    private val secureRandom = SecureRandom()
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val base64UrlEncoder = Base64.getUrlEncoder().withoutPadding()
    private val base64UrlDecoder = Base64.getUrlDecoder()

    fun deriveKek(
        passphrase: CharArray,
        saltBase64: String = randomBase64Url(16),
        iterations: Int = DEFAULT_PBKDF2_ITERATIONS,
        keyLengthBits: Int = DEFAULT_PBKDF2_KEY_LENGTH_BITS
    ): MemoKeyDerivation {
        require(passphrase.isNotEmpty()) { "비밀문구를 입력해주세요." }
        val salt = decodeBase64Url(saltBase64)
        val keySpec = PBEKeySpec(passphrase, salt, iterations, keyLengthBits)
        val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = keyFactory.generateSecret(keySpec).encoded
        keySpec.clearPassword()
        return MemoKeyDerivation(
            saltBase64 = saltBase64,
            iterations = iterations,
            keyLengthBits = keyLengthBits,
            keyBytes = keyBytes
        )
    }

    fun generateDek(): ByteArray = randomBytes(KEY_SIZE_BYTES)

    fun encryptMemo(
        memo: String,
        dekBytes: ByteArray,
        userId: String,
        eventId: String,
        localDate: String
    ): String {
        return encryptPayload(
            plainBytes = memo.toByteArray(Charsets.UTF_8),
            keyBytes = dekBytes,
            aad = buildAad(userId, eventId, localDate)
        )
    }

    fun decryptMemo(
        payload: String,
        dekBytes: ByteArray,
        userId: String,
        eventId: String,
        localDate: String
    ): String {
        val plainBytes = decryptPayload(
            payload = payload,
            keyBytes = dekBytes,
            aad = buildAad(userId, eventId, localDate)
        )
        return plainBytes.toString(Charsets.UTF_8)
    }

    fun wrapDek(dekBytes: ByteArray, kekBytes: ByteArray): String {
        return encryptPayload(
            plainBytes = dekBytes,
            keyBytes = kekBytes,
            aad = null
        )
    }

    fun unwrapDek(wrappedDek: String, kekBytes: ByteArray): ByteArray {
        return decryptPayload(
            payload = wrappedDek,
            keyBytes = kekBytes,
            aad = null
        )
    }

    private fun encryptPayload(
        plainBytes: ByteArray,
        keyBytes: ByteArray,
        aad: String?
    ): String {
        val nonce = randomBytes(GCM_NONCE_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = SecretKeySpec(keyBytes, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, nonce))
        aad?.let { cipher.updateAAD(it.toByteArray(Charsets.UTF_8)) }
        val encrypted = cipher.doFinal(plainBytes)
        val cipherText = encrypted.copyOfRange(0, encrypted.size - (GCM_TAG_BITS / 8))
        val tag = encrypted.copyOfRange(encrypted.size - (GCM_TAG_BITS / 8), encrypted.size)
        return json.encodeToString(
            MemoCipherEnvelope(
                v = 1,
                alg = "A256GCM",
                kid = "user-key-v1",
                nonce = encodeBase64Url(nonce),
                ct = encodeBase64Url(cipherText),
                tag = encodeBase64Url(tag)
            )
        )
    }

    private fun decryptPayload(
        payload: String,
        keyBytes: ByteArray,
        aad: String?
    ): ByteArray {
        val envelope = json.decodeFromString<MemoCipherEnvelope>(payload)
        val nonce = decodeBase64Url(envelope.nonce)
        val cipherText = decodeBase64Url(envelope.ct)
        val tag = decodeBase64Url(envelope.tag)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = SecretKeySpec(keyBytes, "AES")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, nonce))
        aad?.let { cipher.updateAAD(it.toByteArray(Charsets.UTF_8)) }
        return cipher.doFinal(cipherText + tag)
    }

    private fun buildAad(userId: String, eventId: String, localDate: String): String {
        return "$userId|$eventId|$localDate"
    }

    private fun randomBytes(size: Int): ByteArray {
        return ByteArray(size).also(secureRandom::nextBytes)
    }

    private fun randomBase64Url(size: Int): String = encodeBase64Url(randomBytes(size))

    private fun encodeBase64Url(value: ByteArray): String = base64UrlEncoder.encodeToString(value)

    private fun decodeBase64Url(value: String): ByteArray = base64UrlDecoder.decode(value)
}
