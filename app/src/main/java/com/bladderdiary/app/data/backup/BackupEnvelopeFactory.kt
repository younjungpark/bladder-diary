package com.bladderdiary.app.data.backup

import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.util.UUID

class BackupEnvelopeFactory(
    private val crypto: BackupCrypto = BackupCrypto(),
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    fun createWithPassword(
        payload: BackupPlainPayloadV1,
        passphrase: CharArray,
        existingDekBytes: ByteArray? = null
    ): BackupEnvelopeCreation {
        BackupPayloadValidator.validate(payload, payload.userId)
        val createdAtEpochMs = clock()
        val payloadId = UUID.randomUUID().toString()
        val dekBytes = existingDekBytes ?: crypto.generateBackupDek()
        val passwordEnvelope = crypto.createPasswordEnvelope(
            dekBytes = dekBytes,
            passphrase = passphrase,
            userId = payload.userId
        )
        return createEnvelope(
            payload = payload,
            payloadId = payloadId,
            createdAtEpochMs = createdAtEpochMs,
            dekBytes = dekBytes,
            passwordEnvelope = passwordEnvelope
        )
    }

    fun createWithStoredKey(
        payload: BackupPlainPayloadV1,
        storedBackupDek: StoredBackupDek
    ): BackupEnvelopeCreation {
        BackupPayloadValidator.validate(payload, payload.userId)
        val createdAtEpochMs = clock()
        val payloadId = UUID.randomUUID().toString()
        return createEnvelope(
            payload = payload,
            payloadId = payloadId,
            createdAtEpochMs = createdAtEpochMs,
            dekBytes = storedBackupDek.dekBytes,
            passwordEnvelope = storedBackupDek.passwordEnvelope
        )
    }

    fun decrypt(
        envelopeJson: String,
        passphrase: CharArray,
        targetUserId: String
    ): BackupEnvelopeDecryption {
        val envelope = runCatching {
            BackupJson.decodeFromString<BackupEnvelopeV1>(envelopeJson)
        }.getOrElse {
            throw BackupValidationException("백업 파일 형식이 올바르지 않습니다.")
        }
        validateEnvelope(envelope)
        val dekBytes = crypto.unwrapBackupDek(
            encryption = envelope.encryption,
            passphrase = passphrase,
            userId = targetUserId
        )
        val payload = crypto.decryptPayload(
            encryptedValue = envelope.payload,
            dekBytes = dekBytes,
            userId = targetUserId,
            payloadId = envelope.payloadId,
            createdAtEpochMs = envelope.createdAtEpochMs
        )
        BackupPayloadValidator.validate(payload, targetUserId)
        return BackupEnvelopeDecryption(
            payload = payload,
            dekBytes = dekBytes,
            passwordEnvelope = envelope.encryption,
            createdAtEpochMs = envelope.createdAtEpochMs,
            appVersionName = envelope.appVersionName,
            appVersionCode = envelope.appVersionCode,
            databaseVersion = envelope.databaseVersion
        )
    }

    private fun createEnvelope(
        payload: BackupPlainPayloadV1,
        payloadId: String,
        createdAtEpochMs: Long,
        dekBytes: ByteArray,
        passwordEnvelope: BackupEncryptionManifest
    ): BackupEnvelopeCreation {
        val encryptedPayload = crypto.encryptPayload(
            payload = payload,
            dekBytes = dekBytes,
            payloadId = payloadId,
            createdAtEpochMs = createdAtEpochMs
        )
        val envelope = BackupEnvelopeV1(
            payloadId = payloadId,
            createdAtEpochMs = createdAtEpochMs,
            appVersionName = payload.sourceAppVersionName,
            appVersionCode = payload.sourceAppVersionCode,
            databaseVersion = payload.sourceDatabaseVersion,
            encryption = passwordEnvelope,
            payload = encryptedPayload
        )
        return BackupEnvelopeCreation(
            envelopeJson = BackupJson.encodeToString(envelope),
            dekBytes = dekBytes,
            passwordEnvelope = passwordEnvelope
        )
    }

    private fun validateEnvelope(envelope: BackupEnvelopeV1) {
        if (envelope.type != BackupEnvelopeV1.BACKUP_TYPE) {
            throw BackupValidationException("지원하지 않는 백업 파일입니다.")
        }
        if (envelope.backupVersion != BackupEnvelopeV1.BACKUP_VERSION) {
            throw BackupValidationException("지원하지 않는 백업 버전입니다.")
        }
        if (envelope.payloadId.isBlank()) {
            throw BackupValidationException("백업 payload 식별자가 비어 있습니다.")
        }
    }
}
