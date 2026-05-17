package com.bladderdiary.app.data.backup

interface BackupDekStore {
    suspend fun save(userId: String, storedBackupDek: StoredBackupDek)
    suspend fun load(userId: String): StoredBackupDek?
    suspend fun clear(userId: String)
}
