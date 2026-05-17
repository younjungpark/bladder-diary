package com.bladderdiary.app.data.backup

import com.bladderdiary.app.data.drive.DriveBackupFileClient
import com.bladderdiary.app.data.drive.DriveBackupFileMetadata

class BackupEngine(
    private val localDataSource: BackupLocalDataSource,
    private val backupDekStore: BackupDekStore,
    private val driveBackupFileClient: DriveBackupFileClient,
    private val envelopeFactory: BackupEnvelopeFactory = BackupEnvelopeFactory()
) {
    suspend fun createBackupEnvelope(userId: String, passphrase: CharArray): Result<String> =
        runCatching {
            val payload = localDataSource.exportPayload(userId)
            val existingDek = backupDekStore.load(userId)?.dekBytes
            val creation = envelopeFactory.createWithPassword(
                payload = payload,
                passphrase = passphrase,
                existingDekBytes = existingDek
            )
            backupDekStore.save(
                userId = userId,
                storedBackupDek = StoredBackupDek(
                    dekBytes = creation.dekBytes,
                    passwordEnvelope = creation.passwordEnvelope
                )
            )
            creation.envelopeJson
        }

    suspend fun createBackupEnvelopeWithStoredKey(userId: String): Result<String> = runCatching {
        val storedBackupDek = backupDekStore.load(userId) ?: throw BackupMissingLocalKeyException()
        val payload = localDataSource.exportPayload(userId)
        envelopeFactory.createWithStoredKey(
            payload = payload,
            storedBackupDek = storedBackupDek
        ).envelopeJson
    }

    suspend fun restoreFromEnvelope(
        userId: String,
        envelopeJson: String,
        passphrase: CharArray,
        mode: BackupRestoreMode = BackupRestoreMode.MERGE
    ): Result<BackupRestoreReport> = runCatching {
        val decryption = envelopeFactory.decrypt(
            envelopeJson = envelopeJson,
            passphrase = passphrase,
            targetUserId = userId
        )
        val report = localDataSource.restorePayload(
            userId = userId,
            payload = decryption.payload,
            mode = mode
        )
        backupDekStore.save(
            userId = userId,
            storedBackupDek = StoredBackupDek(
                dekBytes = decryption.dekBytes,
                passwordEnvelope = decryption.passwordEnvelope
            )
        )
        report
    }

    suspend fun uploadLatestBackup(
        accessToken: String,
        userId: String,
        passphrase: CharArray
    ): Result<DriveBackupFileMetadata> = runCatching {
        val envelopeJson = createBackupEnvelope(
            userId = userId,
            passphrase = passphrase
        ).getOrThrow()
        driveBackupFileClient.uploadLatestBackup(
            accessToken = accessToken,
            backupJson = envelopeJson
        )
    }

    suspend fun uploadLatestBackupWithStoredKey(
        accessToken: String,
        userId: String
    ): Result<DriveBackupFileMetadata> = runCatching {
        val envelopeJson = createBackupEnvelopeWithStoredKey(userId).getOrThrow()
        driveBackupFileClient.uploadLatestBackup(
            accessToken = accessToken,
            backupJson = envelopeJson
        )
    }

    suspend fun restoreLatestBackup(
        accessToken: String,
        userId: String,
        passphrase: CharArray,
        mode: BackupRestoreMode = BackupRestoreMode.MERGE
    ): Result<BackupRestoreReport> = runCatching {
        val envelopeJson = driveBackupFileClient.downloadLatestBackup(accessToken)
        restoreFromEnvelope(
            userId = userId,
            envelopeJson = envelopeJson,
            passphrase = passphrase,
            mode = mode
        ).getOrThrow()
    }
}
