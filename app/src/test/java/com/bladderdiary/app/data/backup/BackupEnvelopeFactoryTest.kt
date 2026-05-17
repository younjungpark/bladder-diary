package com.bladderdiary.app.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupEnvelopeFactoryTest {
    private val factory = BackupEnvelopeFactory(clock = { 1_777_000_000_000L })

    @Test
    fun `백업 envelope 생성 후 같은 비밀번호로 payload를 복원한다`() {
        val payload = backupPayload()
        val creation = factory.createWithPassword(
            payload = payload,
            passphrase = "backup-secret".toCharArray()
        )

        val restored = factory.decrypt(
            envelopeJson = creation.envelopeJson,
            passphrase = "backup-secret".toCharArray(),
            targetUserId = "user-1"
        )

        assertEquals(payload, restored.payload)
        assertEquals(creation.dekBytes.toList(), restored.dekBytes.toList())
        assertEquals(creation.passwordEnvelope, restored.passwordEnvelope)
    }

    @Test
    fun `백업 envelope에는 민감 기록 평문이 직접 노출되지 않는다`() {
        val creation = factory.createWithPassword(
            payload = backupPayload(),
            passphrase = "backup-secret".toCharArray()
        )

        assertFalse(creation.envelopeJson.contains("민감메모XYZ"))
        assertFalse(creation.envelopeJson.contains("\"volumeMl\":250"))
        assertFalse(creation.envelopeJson.contains("\"urgency\":4"))
    }

    @Test
    fun `비밀번호가 다르면 복원에 실패한다`() {
        val creation = factory.createWithPassword(
            payload = backupPayload(),
            passphrase = "backup-secret".toCharArray()
        )

        val error = runCatching {
            factory.decrypt(
                envelopeJson = creation.envelopeJson,
                passphrase = "wrong-secret".toCharArray(),
                targetUserId = "user-1"
            )
        }.exceptionOrNull()

        assertTrue(error is BackupCryptoException)
    }

    @Test
    fun `대상 사용자가 다르면 AAD 검증으로 복원에 실패한다`() {
        val creation = factory.createWithPassword(
            payload = backupPayload(),
            passphrase = "backup-secret".toCharArray()
        )

        val error = runCatching {
            factory.decrypt(
                envelopeJson = creation.envelopeJson,
                passphrase = "backup-secret".toCharArray(),
                targetUserId = "other-user"
            )
        }.exceptionOrNull()

        assertTrue(error is BackupCryptoException)
    }

    private fun backupPayload(): BackupPlainPayloadV1 = BackupPlainPayloadV1(
        exportedAtEpochMs = 1_777_000_000_000L,
        sourceAppVersionName = "1.0.4",
        sourceAppVersionCode = 14,
        sourceDatabaseVersion = 8,
        userId = "user-1",
        records = listOf(
            BackupRecordV1(
                localId = "record-1",
                voidedAtEpochMs = 1_776_999_900_000L,
                localDate = "2026-04-20",
                isDeleted = false,
                updatedAtEpochMs = 1_776_999_950_000L,
                memo = "민감메모XYZ",
                volumeMl = 250,
                urgency = 4,
                hasIncontinence = true,
                isNocturia = true
            )
        )
    )
}
