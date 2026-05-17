package com.bladderdiary.app.data.backup

interface BackupLocalDataSource {
    suspend fun exportPayload(userId: String): BackupPlainPayloadV1

    suspend fun restorePayload(
        userId: String,
        payload: BackupPlainPayloadV1,
        mode: BackupRestoreMode
    ): BackupRestoreReport
}
