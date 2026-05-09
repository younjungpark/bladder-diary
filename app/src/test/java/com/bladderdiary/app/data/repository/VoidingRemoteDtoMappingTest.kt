package com.bladderdiary.app.data.repository

import com.bladderdiary.app.data.local.VoidingEventEntity
import com.bladderdiary.app.data.remote.SupabaseJson
import com.bladderdiary.app.data.security.MemoEncryptionScheme
import com.bladderdiary.app.data.security.RecordEncryptionScheme
import com.bladderdiary.app.domain.model.SyncState
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoidingRemoteDtoMappingTest {
    @Test
    fun `기록 본문 암호화 업로드 DTO는 민감 평문 필드를 비운다`() {
        val entity = sampleEntity().copy(
            recordCiphertext = "record-ciphertext",
            recordEncryption = RecordEncryptionScheme.E2EE_RECORD_V1
        )

        val dto = entity.toRemoteDtoForUpload()

        assertEquals("2026-03-06T00:00:00Z", dto.voidedAt)
        assertNull(dto.volumeMl)
        assertNull(dto.urgency)
        assertFalse(dto.hasIncontinence)
        assertFalse(dto.isNocturia)
        assertNull(dto.memoCiphertext)
        assertEquals(MemoEncryptionScheme.NONE, dto.memoEncryption)
        assertEquals("record-ciphertext", dto.recordCiphertext)
        assertEquals(RecordEncryptionScheme.E2EE_RECORD_V1, dto.recordEncryption)
    }

    @Test
    fun `기록 본문 암호화 업로드 JSON은 기존 민감 컬럼을 명시적으로 비운다`() {
        val entity = sampleEntity().copy(
            recordCiphertext = "record-ciphertext",
            recordEncryption = RecordEncryptionScheme.E2EE_RECORD_V1
        )

        val json = SupabaseJson.encodeToString(listOf(entity.toRemoteDtoForUpload()))

        assertTrue(json.contains("\"volume_ml\":null"))
        assertTrue(json.contains("\"urgency\":null"))
        assertTrue(json.contains("\"has_incontinence\":false"))
        assertTrue(json.contains("\"is_nocturia\":false"))
        assertTrue(json.contains("\"memo_ciphertext\":null"))
        assertTrue(json.contains("\"memo_encryption\":\"NONE\""))
        assertTrue(json.contains("\"record_ciphertext\":\"record-ciphertext\""))
        assertTrue(json.contains("\"record_encryption\":\"E2EE_RECORD_V1\""))
    }

    @Test
    fun `legacy 업로드 DTO는 기존 필드를 유지한다`() {
        val entity = sampleEntity()

        val dto = entity.toRemoteDtoForUpload()

        assertEquals(Instant.fromEpochMilliseconds(1_777_777_777_000L).toString(), dto.voidedAt)
        assertEquals(250, dto.volumeMl)
        assertEquals(4, dto.urgency)
        assertEquals(true, dto.hasIncontinence)
        assertEquals(true, dto.isNocturia)
        assertEquals("memo-ciphertext", dto.memoCiphertext)
        assertEquals(MemoEncryptionScheme.NONE, dto.memoEncryption)
        assertNull(dto.recordCiphertext)
        assertEquals(RecordEncryptionScheme.NONE, dto.recordEncryption)
    }

    private fun sampleEntity(): VoidingEventEntity = VoidingEventEntity(
        localId = "event-1",
        userId = "user-1",
        voidedAtEpochMs = 1_777_777_777_000L,
        localDate = "2026-03-06",
        isDeleted = false,
        syncState = SyncState.PENDING_CREATE,
        updatedAtEpochMs = 1_777_777_777_000L,
        memo = "memo",
        volumeMl = 250,
        urgency = 4,
        hasIncontinence = true,
        isNocturia = true,
        memoCiphertext = "memo-ciphertext",
        memoEncryption = MemoEncryptionScheme.NONE
    )
}
