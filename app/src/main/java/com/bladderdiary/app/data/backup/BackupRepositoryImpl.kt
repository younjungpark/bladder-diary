package com.bladderdiary.app.data.backup

import com.bladderdiary.app.data.drive.DriveAuthorizationClient
import com.bladderdiary.app.data.drive.DriveAuthorizationResult
import com.bladderdiary.app.domain.model.AuthRepository
import com.bladderdiary.app.domain.model.BackupRepository
import com.bladderdiary.app.domain.model.BackupRestorePreview
import com.bladderdiary.app.domain.model.BackupSettingsState
import com.bladderdiary.app.worker.BackupWorkScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class BackupRepositoryImpl(
    private val authRepository: AuthRepository,
    private val backupEngine: BackupEngine,
    private val preferenceStore: BackupPreferenceStore,
    private val scheduler: BackupWorkScheduler,
    private val driveAuthorizationClient: DriveAuthorizationClient,
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) : BackupRepository {
    private val activeBackupCount = MutableStateFlow(0)
    private val restoreMutex = Mutex()
    private var pendingRestore: PendingRestore? = null

    override fun observeState(): Flow<BackupSettingsState> =
        authRepository.sessionFlow.flatMapLatest { session ->
            if (session == null) {
                flowOf(BackupSettingsState())
            } else {
                combine(
                    preferenceStore.observe(session.userId),
                    activeBackupCount.map { it > 0 }
                ) { storedState, isRunning ->
                    storedState.copy(isBackupRunning = isRunning)
                }
            }
        }

    override suspend fun backupToDrive(accessToken: String, passphrase: CharArray): Result<Unit> =
        withBackupInProgress {
            val userId = currentUserId()
            val result = backupEngine.uploadLatestBackup(
                accessToken = accessToken,
                userId = userId,
                passphrase = passphrase
            ).map { Unit }
            recordBackupResult(userId, result)
            result
        }

    override suspend fun backupToDriveWithStoredKey(accessToken: String): Result<Unit> =
        withBackupInProgress {
            val userId = currentUserId()
            val result = backupEngine.uploadLatestBackupWithStoredKey(
                accessToken = accessToken,
                userId = userId
            ).map { Unit }
            recordBackupResult(userId, result)
            result
        }

    override suspend fun createManualBackup(passphrase: CharArray): Result<String> =
        withBackupInProgress {
            val userId = currentUserId()
            backupEngine.createBackupEnvelope(
                userId = userId,
                passphrase = passphrase
            )
        }

    override suspend fun createManualBackupWithStoredKey(): Result<String> = withBackupInProgress {
        val userId = currentUserId()
        backupEngine.createBackupEnvelopeWithStoredKey(userId)
    }

    override suspend fun prepareDriveRestore(
        accessToken: String,
        passphrase: CharArray
    ): Result<BackupRestorePreview> = withBackupInProgress {
        val userId = currentUserId()
        backupEngine.downloadAndDecryptLatestBackup(
            accessToken = accessToken,
            userId = userId,
            passphrase = passphrase
        ).mapCatching { decryption ->
            cachePendingRestore(userId, decryption)
        }
    }

    override suspend fun prepareManualRestore(
        envelopeJson: String,
        passphrase: CharArray
    ): Result<BackupRestorePreview> = withBackupInProgress {
        val userId = currentUserId()
        backupEngine.decryptEnvelope(
            userId = userId,
            envelopeJson = envelopeJson,
            passphrase = passphrase
        ).mapCatching { decryption ->
            cachePendingRestore(userId, decryption)
        }
    }

    override suspend fun confirmPendingRestore(
        mode: BackupRestoreMode
    ): Result<BackupRestoreReport> = withBackupInProgress {
        val restore = restoreMutex.withLock {
            pendingRestore ?: throw IllegalStateException("복원할 백업 미리보기가 없습니다.")
        }
        val result = backupEngine.restoreDecrypted(
            userId = restore.userId,
            decryption = restore.decryption,
            mode = mode
        )
        if (result.isSuccess) {
            restoreMutex.withLock {
                if (pendingRestore?.preview?.previewId == restore.preview.previewId) {
                    pendingRestore = null
                }
            }
        }
        result
    }

    override suspend fun cancelPendingRestore() {
        restoreMutex.withLock {
            pendingRestore = null
        }
    }

    override suspend fun setAutoBackupEnabled(isEnabled: Boolean): Result<Unit> = runCatching {
        val userId = currentUserId()
        if (isEnabled) {
            val state = preferenceStore.read(userId)
            if (!state.isDriveBackupConnected) {
                throw BackupPermissionException("먼저 백업 비밀번호로 Google Drive 백업을 한 번 만들어주세요.")
            }
            preferenceStore.setAutoBackupEnabled(userId, true)
            scheduler.request()
        } else {
            preferenceStore.setAutoBackupEnabled(userId, false)
            scheduler.cancel()
        }
    }

    override suspend fun runAutomaticBackup(): Result<Unit> = withBackupInProgress {
        val session =
            authRepository.getSession() ?: return@withBackupInProgress Result.success(Unit)
        val settings = preferenceStore.read(session.userId)
        if (!settings.isAutoBackupEnabled) {
            return@withBackupInProgress Result.success(Unit)
        }
        val authResult = driveAuthorizationClient.authorizeDriveAppData()
        val accessToken = when (authResult) {
            is DriveAuthorizationResult.Authorized -> authResult.token.accessToken

            is DriveAuthorizationResult.RequiresUserResolution -> {
                return@withBackupInProgress recordAutomaticFailure(
                    userId = session.userId,
                    error = BackupPermissionException("Google Drive 권한을 다시 확인해 주세요.")
                )
            }

            DriveAuthorizationResult.Denied -> {
                return@withBackupInProgress recordAutomaticFailure(
                    userId = session.userId,
                    error = BackupPermissionException("Google Drive 권한이 거부되었습니다.")
                )
            }

            is DriveAuthorizationResult.Unavailable -> {
                return@withBackupInProgress recordAutomaticFailure(
                    userId = session.userId,
                    error = BackupPermissionException(authResult.reason)
                )
            }
        }
        val result = backupEngine.uploadLatestBackupWithStoredKey(
            accessToken = accessToken,
            userId = session.userId
        ).map { Unit }
        recordBackupResult(session.userId, result)
        result
    }

    private suspend fun currentUserId(): String =
        authRepository.getSession()?.userId ?: throw IllegalStateException("로그인이 필요합니다.")

    private suspend fun cachePendingRestore(
        userId: String,
        decryption: BackupEnvelopeDecryption
    ): BackupRestorePreview {
        val preview = decryption.toPreview()
        restoreMutex.withLock {
            pendingRestore = PendingRestore(
                userId = userId,
                preview = preview,
                decryption = decryption
            )
        }
        return preview
    }

    private fun BackupEnvelopeDecryption.toPreview(): BackupRestorePreview = BackupRestorePreview(
        previewId = UUID.randomUUID().toString(),
        createdAtEpochMs = createdAtEpochMs,
        sourceAppVersionName = appVersionName,
        sourceAppVersionCode = appVersionCode,
        sourceDatabaseVersion = databaseVersion,
        recordCount = payload.records.size,
        deletedRecordCount = payload.records.count { it.isDeleted }
    )

    private suspend fun recordBackupResult(userId: String, result: Result<Unit>) {
        if (result.isSuccess) {
            preferenceStore.markBackupSuccess(userId, clock())
        } else {
            preferenceStore.markBackupFailure(
                userId = userId,
                failedAtEpochMs = clock(),
                message = result.exceptionOrNull()?.message
            )
        }
    }

    private suspend fun recordAutomaticFailure(userId: String, error: Throwable): Result<Unit> {
        val result = Result.failure<Unit>(error)
        recordBackupResult(userId, result)
        return result
    }

    private suspend fun <T> withBackupInProgress(block: suspend () -> Result<T>): Result<T> {
        activeBackupCount.update { it + 1 }
        return try {
            block()
        } catch (error: Throwable) {
            Result.failure(error)
        } finally {
            activeBackupCount.update { count -> (count - 1).coerceAtLeast(0) }
        }
    }

    private data class PendingRestore(
        val userId: String,
        val preview: BackupRestorePreview,
        val decryption: BackupEnvelopeDecryption
    )
}
