package com.bladderdiary.app.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PinCryptoTest {
    @Test
    fun `같은 PIN과 salt면 해시가 동일하다`() {
        val salt = PinCrypto.generateSaltBase64()
        val a = PinCrypto.hashPinBase64("1234", salt)
        val b = PinCrypto.hashPinBase64("1234", salt)

        assertEquals(a, b)
    }

    @Test
    fun `salt가 다르면 해시가 달라진다`() {
        val a = PinCrypto.hashPinBase64("1234", PinCrypto.generateSaltBase64())
        val b = PinCrypto.hashPinBase64("1234", PinCrypto.generateSaltBase64())

        assertNotEquals(a, b)
    }

    @Test
    fun `PIN이 다르면 해시가 달라진다`() {
        val salt = PinCrypto.generateSaltBase64()
        val a = PinCrypto.hashPinBase64("1234", salt)
        val b = PinCrypto.hashPinBase64("9999", salt)

        assertNotEquals(a, b)
    }
}
