package com.bladderdiary.app.data.backup

import com.bladderdiary.app.BuildConfig
import com.bladderdiary.app.data.local.AppDatabase

data class BackupAppMetadata(
    val versionName: String,
    val versionCode: Int,
    val databaseVersion: Int
)

interface BackupAppMetadataProvider {
    fun current(): BackupAppMetadata
}

class DefaultBackupAppMetadataProvider : BackupAppMetadataProvider {
    override fun current(): BackupAppMetadata = BackupAppMetadata(
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        databaseVersion = AppDatabase.DATABASE_VERSION
    )
}
