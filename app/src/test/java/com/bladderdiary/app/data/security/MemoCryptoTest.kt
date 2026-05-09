package com.bladderdiary.app.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MemoCryptoTest {
    @Test
    fun `같은 비밀문구와 salt로 동일한 KEK를 만든다`() {
        val salt = MemoCrypto.deriveKek("secret-passphrase".toCharArray()).saltBase64

        val first = MemoCrypto.deriveKek(
            passphrase = "secret-passphrase".toCharArray(),
            saltBase64 = salt
        )
        val second = MemoCrypto.deriveKek(
            passphrase = "secret-passphrase".toCharArray(),
            saltBase64 = salt
        )

        assertEquals(first.keyBytes.toList(), second.keyBytes.toList())
    }

    @Test
    fun `메모 암복호화가 왕복된다`() {
        val dek = MemoCrypto.generateDek()
        val payload = MemoCrypto.encryptMemo(
            memo = "야간 빈뇨가 심해졌음",
            dekBytes = dek,
            userId = "user-1",
            eventId = "event-1",
            localDate = "2026-03-06"
        )

        val restored = MemoCrypto.decryptMemo(
            payload = payload,
            dekBytes = dek,
            userId = "user-1",
            eventId = "event-1",
            localDate = "2026-03-06"
        )

        assertEquals("야간 빈뇨가 심해졌음", restored)
    }

    @Test
    fun `AAD가 다르면 복호화에 실패한다`() {
        val dek = MemoCrypto.generateDek()
        val payload = MemoCrypto.encryptMemo(
            memo = "복호화 테스트",
            dekBytes = dek,
            userId = "user-1",
            eventId = "event-1",
            localDate = "2026-03-06"
        )

        val error = runCatching {
            MemoCrypto.decryptMemo(
                payload = payload,
                dekBytes = dek,
                userId = "user-1",
                eventId = "event-1",
                localDate = "2026-03-07"
            )
        }.exceptionOrNull()

        assertNotEquals(null, error)
    }

    @Test
    fun `기록 본문 암복호화가 왕복된다`() {
        val dek = MemoCrypto.generateDek()
        val record = VoidingRecordPlainPayload(
            voidedAtEpochMs = 1_777_777_777_000L,
            volumeMl = 250,
            urgency = 4,
            hasIncontinence = true,
            isNocturia = true,
            memo = "민감평문XYZ"
        )

        val payload = MemoCrypto.encryptRecord(
            record = record,
            dekBytes = dek,
            userId = "user-1",
            eventId = "event-1",
            localDate = "2026-03-06"
        )
        val restored = MemoCrypto.decryptRecord(
            payload = payload,
            dekBytes = dek,
            userId = "user-1",
            eventId = "event-1",
            localDate = "2026-03-06"
        )

        assertEquals(record, restored)
    }

    @Test
    fun `기록 본문 AAD가 다르면 복호화에 실패한다`() {
        val dek = MemoCrypto.generateDek()
        val payload = MemoCrypto.encryptRecord(
            record = VoidingRecordPlainPayload(
                voidedAtEpochMs = 1_777_777_777_000L,
                volumeMl = 250,
                urgency = 4,
                memo = "복호화 실패 테스트"
            ),
            dekBytes = dek,
            userId = "user-1",
            eventId = "event-1",
            localDate = "2026-03-06"
        )

        val error = runCatching {
            MemoCrypto.decryptRecord(
                payload = payload,
                dekBytes = dek,
                userId = "user-1",
                eventId = "event-2",
                localDate = "2026-03-06"
            )
        }.exceptionOrNull()

        assertNotEquals(null, error)
    }

    @Test
    fun `기록 암호문은 민감 평문을 직접 포함하지 않는다`() {
        val dek = MemoCrypto.generateDek()

        val payload = MemoCrypto.encryptRecord(
            record = VoidingRecordPlainPayload(
                voidedAtEpochMs = 1_777_777_777_000L,
                volumeMl = 250,
                urgency = 4,
                hasIncontinence = true,
                isNocturia = true,
                memo = "민감평문XYZ"
            ),
            dekBytes = dek,
            userId = "user-1",
            eventId = "event-1",
            localDate = "2026-03-06"
        )

        assertFalse(payload.contains("민감평문XYZ"))
    }

    @Test
    fun `같은 DEK를 새 비밀문구로 다시 감싸도 복원된다`() {
        val dek = MemoCrypto.generateDek()
        val firstDerived = MemoCrypto.deriveKek("old-passphrase".toCharArray())
        val secondDerived = MemoCrypto.deriveKek("new-passphrase".toCharArray())
        val firstWrapped = MemoCrypto.wrapDek(dek, firstDerived.keyBytes)
        val unwrapped = MemoCrypto.unwrapDek(firstWrapped, firstDerived.keyBytes)
        val secondWrapped = MemoCrypto.wrapDek(unwrapped, secondDerived.keyBytes)

        val restored = MemoCrypto.unwrapDek(secondWrapped, secondDerived.keyBytes)

        assertEquals(dek.toList(), restored.toList())
    }
}
