package com.bladderdiary.app.data.drive

data class DriveBackupFileMetadata(
    val fileId: String,
    val fileName: String,
    val modifiedTime: String? = null
)

interface DriveBackupFileClient {
    suspend fun uploadLatestBackup(accessToken: String, backupJson: String): DriveBackupFileMetadata

    suspend fun downloadLatestBackup(accessToken: String): String
}
