package com.bladderdiary.app.domain.model

import com.bladderdiary.app.data.backup.BackupRestoreMode
import com.bladderdiary.app.data.backup.BackupRestoreReport
import kotlinx.coroutines.flow.Flow

data class BackupSettingsState(
    val isDriveBackupConnected: Boolean = false,
    val isAutoBackupEnabled: Boolean = false,
    val lastBackupSuccessEpochMs: Long? = null,
    val lastBackupFailureEpochMs: Long? = null,
    val lastBackupErrorMessage: String? = null,
    val isBackupRunning: Boolean = false
)

data class BackupRestorePreview(
    val previewId: String,
    val createdAtEpochMs: Long,
    val sourceAppVersionName: String,
    val sourceAppVersionCode: Int,
    val sourceDatabaseVersion: Int,
    val recordCount: Int,
    val deletedRecordCount: Int
)

interface BackupRepository {
    fun observeState(): Flow<BackupSettingsState>

    suspend fun backupToDrive(accessToken: String, passphrase: CharArray): Result<Unit>

    suspend fun backupToDriveWithStoredKey(accessToken: String): Result<Unit>

    suspend fun createManualBackup(passphrase: CharArray): Result<String>

    suspend fun createManualBackupWithStoredKey(): Result<String>

    suspend fun prepareDriveRestore(
        accessToken: String,
        passphrase: CharArray
    ): Result<BackupRestorePreview>

    suspend fun prepareManualRestore(
        envelopeJson: String,
        passphrase: CharArray
    ): Result<BackupRestorePreview>

    suspend fun confirmPendingRestore(mode: BackupRestoreMode): Result<BackupRestoreReport>

    suspend fun cancelPendingRestore()

    suspend fun setAutoBackupEnabled(isEnabled: Boolean): Result<Unit>

    suspend fun runAutomaticBackup(): Result<Unit>
}
