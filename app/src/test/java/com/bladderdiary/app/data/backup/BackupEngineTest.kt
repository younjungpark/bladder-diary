package com.bladderdiary.app.data.backup

import com.bladderdiary.app.data.drive.DriveBackupFileClient
import com.bladderdiary.app.data.drive.DriveBackupFileMetadata
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupEngineTest {
    @Test
    fun `비밀번호 백업은 Drive client에 암호화 envelope를 업로드하고 로컬 DEK를 저장한다`() = runTest {
        val localDataSource = FakeBackupLocalDataSource(payload = backupPayload())
        val dekStore = FakeBackupDekStore()
        val driveClient = FakeDriveBackupFileClient()
        val engine = BackupEngine(
            localDataSource = localDataSource,
            backupDekStore = dekStore,
            driveBackupFileClient = driveClient,
            envelopeFactory = BackupEnvelopeFactory(clock = { 1_777_000_000_000L })
        )

        val metadata = engine.uploadLatestBackup(
            accessToken = "drive-token",
            userId = "user-1",
            passphrase = "backup-secret".toCharArray()
        ).getOrThrow()

        assertEquals("drive-token", driveClient.lastUploadToken)
        assertEquals("backup-file", metadata.fileId)
        assertNotNull(driveClient.backupJson)
        assertNotNull(dekStore.load("user-1"))
        assertTrue(localDataSource.restoreCalls.isEmpty())
    }

    @Test
    fun `Drive에서 받은 envelope를 복호화한 뒤 기본 병합 모드로 복원한다`() = runTest {
        val payload = backupPayload()
        val sourceLocalDataSource = FakeBackupLocalDataSource(payload = payload)
        val sourceDekStore = FakeBackupDekStore()
        val driveClient = FakeDriveBackupFileClient()
        val sourceEngine = BackupEngine(
            localDataSource = sourceLocalDataSource,
            backupDekStore = sourceDekStore,
            driveBackupFileClient = driveClient,
            envelopeFactory = BackupEnvelopeFactory(clock = { 1_777_000_000_000L })
        )
        sourceEngine.uploadLatestBackup(
            accessToken = "drive-token",
            userId = "user-1",
            passphrase = "backup-secret".toCharArray()
        ).getOrThrow()

        val restoreLocalDataSource = FakeBackupLocalDataSource(payload = payload)
        val restoreDekStore = FakeBackupDekStore()
        val restoreEngine = BackupEngine(
            localDataSource = restoreLocalDataSource,
            backupDekStore = restoreDekStore,
            driveBackupFileClient = driveClient,
            envelopeFactory = BackupEnvelopeFactory(clock = { 1_777_000_000_000L })
        )

        val report = restoreEngine.restoreLatestBackup(
            accessToken = "drive-token",
            userId = "user-1",
            passphrase = "backup-secret".toCharArray()
        ).getOrThrow()

        assertEquals(BackupRestoreMode.MERGE, report.mode)
        assertEquals("drive-token", driveClient.lastDownloadToken)
        assertEquals(BackupRestoreMode.MERGE, restoreLocalDataSource.restoreCalls.single().mode)
        assertEquals(payload, restoreLocalDataSource.restoreCalls.single().payload)
        assertNotNull(restoreDekStore.load("user-1"))
    }

    @Test
    fun `저장된 로컬 DEK가 없으면 자동 백업 envelope 생성은 실패한다`() = runTest {
        val engine = BackupEngine(
            localDataSource = FakeBackupLocalDataSource(payload = backupPayload()),
            backupDekStore = FakeBackupDekStore(),
            driveBackupFileClient = FakeDriveBackupFileClient(),
            envelopeFactory = BackupEnvelopeFactory(clock = { 1_777_000_000_000L })
        )

        val error = engine.createBackupEnvelopeWithStoredKey("user-1").exceptionOrNull()

        assertTrue(error is BackupMissingLocalKeyException)
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
                urgency = 4
            )
        )
    )
}

private class FakeBackupLocalDataSource(private val payload: BackupPlainPayloadV1) :
    BackupLocalDataSource {
    val restoreCalls = mutableListOf<RestoreCall>()

    override suspend fun exportPayload(userId: String): BackupPlainPayloadV1 {
        assertEquals(payload.userId, userId)
        return payload
    }

    override suspend fun restorePayload(
        userId: String,
        payload: BackupPlainPayloadV1,
        mode: BackupRestoreMode
    ): BackupRestoreReport {
        restoreCalls += RestoreCall(userId = userId, payload = payload, mode = mode)
        return BackupRestoreReport(
            mode = mode,
            restoredCount = payload.records.size,
            replacedExistingCount = 0,
            keptLocalNewerCount = 0,
            deletedExistingCount = 0
        )
    }

    data class RestoreCall(
        val userId: String,
        val payload: BackupPlainPayloadV1,
        val mode: BackupRestoreMode
    )
}

private class FakeBackupDekStore : BackupDekStore {
    private val map = mutableMapOf<String, StoredBackupDek>()

    override suspend fun save(userId: String, storedBackupDek: StoredBackupDek) {
        map[userId] = storedBackupDek
    }

    override suspend fun load(userId: String): StoredBackupDek? = map[userId]

    override suspend fun clear(userId: String) {
        map.remove(userId)
    }
}

private class FakeDriveBackupFileClient : DriveBackupFileClient {
    var backupJson: String? = null
    var lastUploadToken: String? = null
    var lastDownloadToken: String? = null

    override suspend fun uploadLatestBackup(
        accessToken: String,
        backupJson: String
    ): DriveBackupFileMetadata {
        lastUploadToken = accessToken
        this.backupJson = backupJson
        return DriveBackupFileMetadata(
            fileId = "backup-file",
            fileName = BACKUP_FILE_NAME,
            modifiedTime = "2026-05-17T00:00:00Z"
        )
    }

    override suspend fun downloadLatestBackup(accessToken: String): String {
        lastDownloadToken = accessToken
        return backupJson ?: throw BackupNotFoundException()
    }
}
