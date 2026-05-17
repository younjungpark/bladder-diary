package com.bladderdiary.app.data.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val BACKUP_FILE_NAME = "bladderdiary-backup-v1.json"
const val BACKUP_FILE_EXTENSION = "bdbackup"

@Serializable
data class BackupEnvelopeV1(
    val type: String = BACKUP_TYPE,
    val backupVersion: Int = BACKUP_VERSION,
    val payloadId: String,
    val createdAtEpochMs: Long,
    val appVersionName: String,
    val appVersionCode: Int,
    val databaseVersion: Int,
    val encryption: BackupEncryptionManifest,
    val payload: BackupEncryptedValue
) {
    companion object {
        const val BACKUP_TYPE = "com.chausoft.bladderdiary.backup"
        const val BACKUP_VERSION = 1
    }
}

@Serializable
data class BackupEncryptionManifest(
    val scheme: String = BACKUP_ENCRYPTION_SCHEME,
    val kdf: String = BACKUP_KDF,
    val kdfSalt: String,
    val kdfParams: BackupKdfParams,
    val wrappedBackupDek: BackupEncryptedValue
) {
    companion object {
        const val BACKUP_ENCRYPTION_SCHEME = "BACKUP_AES_GCM_V1"
        const val BACKUP_KDF = "PBKDF2WithHmacSHA256"
    }
}

@Serializable
data class BackupKdfParams(val iterations: Int, val keyLengthBits: Int)

@Serializable
data class BackupEncryptedValue(val nonce: String, val ciphertext: String, val tag: String)

@Serializable
data class BackupPlainPayloadV1(
    val exportedAtEpochMs: Long,
    val sourceAppVersionName: String,
    val sourceAppVersionCode: Int,
    val sourceDatabaseVersion: Int,
    val userId: String,
    val records: List<BackupRecordV1>
)

@Serializable
data class BackupRecordV1(
    val localId: String,
    val voidedAtEpochMs: Long,
    val localDate: String,
    val isDeleted: Boolean,
    val updatedAtEpochMs: Long,
    val memo: String? = null,
    val volumeMl: Int? = null,
    val urgency: Int? = null,
    val hasIncontinence: Boolean = false,
    val isNocturia: Boolean = false
)

enum class BackupRestoreMode {
    @SerialName("merge")
    MERGE,

    @SerialName("replace")
    REPLACE
}

data class BackupRestoreReport(
    val mode: BackupRestoreMode,
    val restoredCount: Int,
    val replacedExistingCount: Int,
    val keptLocalNewerCount: Int,
    val deletedExistingCount: Int
)

data class StoredBackupDek(
    val dekBytes: ByteArray,
    val passwordEnvelope: BackupEncryptionManifest
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StoredBackupDek) return false
        return dekBytes.contentEquals(other.dekBytes) &&
            passwordEnvelope == other.passwordEnvelope
    }

    override fun hashCode(): Int = 31 * dekBytes.contentHashCode() + passwordEnvelope.hashCode()
}

data class BackupEnvelopeCreation(
    val envelopeJson: String,
    val dekBytes: ByteArray,
    val passwordEnvelope: BackupEncryptionManifest
)

data class BackupEnvelopeDecryption(
    val payload: BackupPlainPayloadV1,
    val dekBytes: ByteArray,
    val passwordEnvelope: BackupEncryptionManifest,
    val createdAtEpochMs: Long,
    val appVersionName: String,
    val appVersionCode: Int,
    val databaseVersion: Int
)
