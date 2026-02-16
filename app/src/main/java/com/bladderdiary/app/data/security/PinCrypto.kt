package com.bladderdiary.app.data.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinCrypto {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_SIZE_BYTES = 16

    fun generateSaltBase64(): String {
        val bytes = ByteArray(SALT_SIZE_BYTES)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hashPinBase64(pin: String, saltBase64: String): String {
        val salt = Base64.getDecoder().decode(saltBase64)
        val keySpec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val secretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM)
        return try {
            val hashBytes = secretKeyFactory.generateSecret(keySpec).encoded
            Base64.getEncoder().encodeToString(hashBytes)
        } finally {
            keySpec.clearPassword()
        }
    }
}
